package hhplus.ecommerce.common.domain.constants;

import java.math.BigDecimal;

/**
 * 비즈니스 상수 정의
 * 최소 충전 금액, 재고 예약 타임아웃 등 전역 상수 관리
 */
public class BusinessConstants {
    // 포인트 관련
    public static final BigDecimal MIN_CHARGE_AMOUNT = BigDecimal.valueOf(1000L); // 최소 충전 금액

    // 재고 관련
    public static final int STOCK_RESERVATION_TIMEOUT_MINUTES = 15; // 재고 예약 타임아웃

    // 쿠폰 관련
    public static final int MAX_RETRY_COUNT = 5; // 외부 연동 최대 재시도 횟수

    // 인기 상품 관련
    public static final int TOP_PRODUCTS_COUNT = 5; // Top N 상품 개수
    public static final int POPULAR_PRODUCTS_DAYS = 3; // 인기 상품 집계 기간 (일)

    private BusinessConstants() {
        throw new AssertionError("유틸리티 클래스는 인스턴스화할 수 없습니다.");
    }
}
