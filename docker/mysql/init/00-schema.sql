-- 스키마 초기화

-- 기존 테이블 삭제 (재실행 시 초기화)
DROP TABLE IF EXISTS popular_products;
DROP TABLE IF EXISTS external_integration_logs;
DROP TABLE IF EXISTS point_histories;
DROP TABLE IF EXISTS user_coupons;
DROP TABLE IF EXISTS coupons;
DROP TABLE IF EXISTS order_items;
DROP TABLE IF EXISTS orders;
DROP TABLE IF EXISTS carts;
DROP TABLE IF EXISTS stock_reservations;
DROP TABLE IF EXISTS stock_histories;
DROP TABLE IF EXISTS product_options;
DROP TABLE IF EXISTS products;
DROP TABLE IF EXISTS users;

-- 사용자 테이블
CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '사용자 고유 ID',
    username VARCHAR(50) NOT NULL UNIQUE COMMENT '사용자명',
    point_balance DECIMAL(15,2) NOT NULL DEFAULT 0 COMMENT '포인트 잔액',
    role VARCHAR(20) NOT NULL DEFAULT 'CUSTOMER' COMMENT '역할: CUSTOMER, ADMIN',
    is_deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '삭제 여부',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_username_deleted (username, is_deleted),
    INDEX idx_id_role_deleted (id, role, is_deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='유저 정보';

-- 상품 기본 정보 테이블
CREATE TABLE products (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '상품 고유 ID',
    product_name VARCHAR(200) NOT NULL COMMENT '상품명',
    category VARCHAR(100) NOT NULL COMMENT '카테고리',
    description TEXT COMMENT '상품 설명',
    price DECIMAL(15,2) NOT NULL COMMENT '기본 상품 가격',
    image_url VARCHAR(500) COMMENT '이미지 URL',
    is_exposed TINYINT(1) NOT NULL DEFAULT 1 COMMENT '노출 여부 (1: 노출, 0: 비노출)',
    is_deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '삭제 여부',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_exposed_deleted_category (is_exposed, is_deleted, category),
    INDEX idx_exposed_deleted_created (is_exposed, is_deleted, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='상품 기본 정보';

-- 상품 옵션 테이블
CREATE TABLE product_options (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '상품 옵션 고유 ID',
    product_id BIGINT NOT NULL COMMENT '상품 ID',
    option_name VARCHAR(100) NOT NULL COMMENT '옵션명',
    price_adjustment DECIMAL(15,2) NOT NULL COMMENT '옵션 가격 조정',
    stock_quantity INT NOT NULL DEFAULT 0 COMMENT '재고 수량',
    is_sold_out TINYINT(1) NOT NULL DEFAULT 0 COMMENT '품절 여부',
    is_exposed TINYINT(1) NOT NULL DEFAULT 1 COMMENT '노출 여부',
    is_deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '삭제 여부',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (product_id) REFERENCES products(id),
    INDEX idx_product_id (product_id),
    INDEX idx_product_soldout_exposed (product_id, is_sold_out, is_exposed, is_deleted),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='상품 옵션 및 재고';

-- 재고 이력 테이블
CREATE TABLE stock_histories (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '재고 이력 고유 ID',
    product_option_id BIGINT NOT NULL COMMENT '상품 옵션 ID',
    amount INT NOT NULL COMMENT '수량',
    adjustment_type VARCHAR(20) NOT NULL COMMENT '재고 조정 타입: ADD(추가), USE(사용)',
    balance INT NOT NULL COMMENT '총 수량',
    description VARCHAR(200) COMMENT '설명',
    updated_by BIGINT NOT NULL COMMENT '수정자 ID',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (product_option_id) REFERENCES product_options(id),
    INDEX idx_option_created (product_option_id, created_at),
    INDEX idx_option_type_created (product_option_id, adjustment_type, created_at),
    INDEX idx_option_updater_created (product_option_id, updated_by, created_at),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='재고 이력';

-- 재고 예약 테이블
CREATE TABLE stock_reservations (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '재고 예약 고유 ID',
    product_option_id BIGINT NOT NULL COMMENT '상품 옵션 ID',
    order_id BIGINT NOT NULL COMMENT '주문 ID',
    reserved_quantity INT NOT NULL COMMENT '예약 수량',
    reservation_status VARCHAR(20) NOT NULL DEFAULT 'RESERVED' COMMENT '예약 상태: RESERVED, CONFIRMED, RELEASED',
    reserved_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '예약 시각',
    expires_at DATETIME NOT NULL COMMENT '예약 만료 시각',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (product_option_id) REFERENCES product_options(id),
    INDEX idx_order_id (order_id),
    INDEX idx_option_status (product_option_id, reservation_status),
    INDEX idx_expires_at (expires_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='재고 예약 (15분 타임아웃)';

-- 장바구니 테이블
CREATE TABLE carts (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '장바구니 고유 ID',
    user_id BIGINT NOT NULL COMMENT '사용자 ID',
    product_option_id BIGINT NOT NULL COMMENT '상품 옵션 ID',
    quantity INT NOT NULL COMMENT '수량',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id),
    FOREIGN KEY (product_option_id) REFERENCES product_options(id),
    UNIQUE KEY uk_user_option (user_id, product_option_id),
    INDEX idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='장바구니';

-- 쿠폰 마스터 테이블
CREATE TABLE coupons (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '쿠폰 고유 ID',
    coupon_name VARCHAR(100) NOT NULL COMMENT '쿠폰명',
    discount_type VARCHAR(20) NOT NULL COMMENT '할인 타입: FIXED(정액), PERCENTAGE(정률)',
    discount_value DECIMAL(15,2) NOT NULL COMMENT '할인 금액 또는 비율',
    min_order_amount DECIMAL(15,2) NOT NULL DEFAULT 0 COMMENT '최소 주문 금액',
    max_issue_count INT NOT NULL COMMENT '최대 발급 수량',
    issued_count INT NOT NULL DEFAULT 0 COMMENT '현재 발급된 수량',
    valid_from DATETIME NOT NULL COMMENT '유효기간 시작일',
    valid_to DATETIME NOT NULL COMMENT '유효기간 종료일',
    created_by BIGINT NOT NULL COMMENT '생성자 ID',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (created_by) REFERENCES users(id),
    INDEX idx_valid_period (valid_from, valid_to),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='쿠폰 마스터';

-- 주문 테이블
CREATE TABLE orders (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '주문 고유 ID',
    order_number VARCHAR(50) NOT NULL UNIQUE COMMENT '주문번호',
    user_id BIGINT NOT NULL COMMENT '사용자 ID',
    total_amount DECIMAL(15,2) NOT NULL COMMENT '총 상품 금액',
    discount_amount DECIMAL(15,2) NOT NULL DEFAULT 0 COMMENT '할인 금액',
    final_amount DECIMAL(15,2) NOT NULL COMMENT '최종 결제 금액',
    payment_method VARCHAR(20) COMMENT '결제 방법: CREDIT(신용카드), CHECK(체크카드), CASH(계좌입금), KAKAO(카카오)',
    coupon_id BIGINT COMMENT '사용한 쿠폰 ID',
    order_status VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '주문 상태: PENDING(결제 대기), PAID(결제 완료), CANCELLED(취소)',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id),
    FOREIGN KEY (coupon_id) REFERENCES coupons(id),
    INDEX idx_user_status (user_id, order_status),
    INDEX idx_user_created (user_id, created_at),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='주문';

CREATE TABLE order_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '주문 상품 고유 ID',
    order_id BIGINT NOT NULL COMMENT '주문 ID',
    product_id BIGINT NOT NULL COMMENT '상품 ID',
    product_option_id BIGINT NOT NULL COMMENT '상품 옵션 ID',
    product_name VARCHAR(200) NOT NULL COMMENT '주문 시점 상품명',
    option_name VARCHAR(100) NOT NULL COMMENT '주문 시점 옵션명',
    unit_price DECIMAL(15,2) NOT NULL COMMENT '단일 상품 가격',
    quantity INT NOT NULL COMMENT '주문 수량',
    subtotal DECIMAL(15,2) NOT NULL COMMENT '소계',
    item_status VARCHAR(20) NOT NULL DEFAULT 'PREPARING' COMMENT '상품 상태: PREPARING(상품준비중), SHIPPING(배송중), DELIVERED(배송완료), CANCELLED(주문취소)',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (order_id) REFERENCES orders(id),
    FOREIGN KEY (product_id) REFERENCES products(id),
    FOREIGN KEY (product_option_id) REFERENCES product_options(id),
    INDEX idx_order_id (order_id),
    INDEX idx_order_status (order_id, item_status),
    INDEX idx_item_status (item_status),
    INDEX idx_product_option_id (product_option_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='주문 상품 상세';

-- 사용자별 쿠폰 테이블
CREATE TABLE user_coupons (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '사용자 쿠폰 고유 ID',
    user_id BIGINT NOT NULL COMMENT '사용자 ID',
    coupon_id BIGINT NOT NULL COMMENT '쿠폰 ID',
    used_at DATETIME COMMENT '사용일시',
    status VARCHAR(20) NOT NULL COMMENT '상태: ACTIVE(사용가능), USED(사용완료), EXPIRED(만료됨)',
    order_id BIGINT COMMENT '사용한 주문 ID',
    issued_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '발급일시',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id),
    FOREIGN KEY (coupon_id) REFERENCES coupons(id),
    FOREIGN KEY (order_id) REFERENCES orders(id),
    UNIQUE KEY uk_user_coupon (user_id, coupon_id),
    INDEX idx_user_status_issued (user_id, status, issued_at),
    INDEX idx_coupon_id (coupon_id),
    INDEX idx_status_issued (status, issued_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='사용자 쿠폰';

-- 포인트 이력 테이블
CREATE TABLE point_histories (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '포인트 이력 고유 ID',
    user_id BIGINT NOT NULL COMMENT '사용자 ID',
    transaction_type VARCHAR(20) NOT NULL COMMENT '거래 타입: CHARGE(충전), USE(사용)',
    amount DECIMAL(15,2) NOT NULL COMMENT '거래 금액',
    balance_after DECIMAL(15,2) NOT NULL COMMENT '거래 후 잔액',
    order_id BIGINT COMMENT '연결된 주문 ID',
    description VARCHAR(200) COMMENT '설명',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id),
    FOREIGN KEY (order_id) REFERENCES orders(id),
    INDEX idx_user_created (user_id, created_at),
    INDEX idx_created_at (created_at),
    INDEX idx_user_type_created (user_id, transaction_type, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='포인트 이력';

-- 외부 시스템 연동 로그 테이블
CREATE TABLE external_integration_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '로그 고유 ID',
    order_id BIGINT NOT NULL COMMENT '주문 ID',
    integration_type VARCHAR(50) NOT NULL COMMENT '연동 타입: LOGISTICS, SALES_MANAGEMENT, ERP',
    is_success TINYINT(1) NOT NULL DEFAULT 0 COMMENT '성공 여부',
    response_message TEXT COMMENT '응답 메시지',
    retry_count INT NOT NULL DEFAULT 0 COMMENT '재시도 횟수',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (order_id) REFERENCES orders(id),
    INDEX idx_order_created (order_id, created_at),
    INDEX idx_type_success_retry (integration_type, is_success, retry_count),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='외부 시스템 연동 로그';

-- 인기 상품 통계 테이블
CREATE TABLE popular_products (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '인기 상품 통계 고유 ID',
    product_id BIGINT NOT NULL COMMENT '상품 ID',
    sales_count INT NOT NULL DEFAULT 0 COMMENT '판매 수량',
    calculation_date DATE NOT NULL COMMENT '집계 기준일',
    rank INT NOT NULL COMMENT '순위',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (product_id) REFERENCES products(id),
    UNIQUE KEY uk_date_rank (calculation_date, rank),
    INDEX idx_product_id (product_id),
    INDEX idx_calculation_date (calculation_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='인기 상품 통계';
