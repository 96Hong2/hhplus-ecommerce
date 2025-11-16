# 🛒 E-Commerce API Specification

> **Version**: 1.1.0 | **Author**: ehkwon

## 1. 개요

이커머스 플랫폼의 RESTful API 명세서입니다. 
8개 도메인(상품, 재고, 유저, 포인트, 장바구니, 주문, 쿠폰, 외부연동)으로 구성되어 있습니다.

**서버 정보**
- 로컬: `http://localhost:8080`
- 운영: `https://api.ecommerce.com`

**주요 특징**

- 주문-결제 프로세스 최적화
- 재고 예약 시스템 (15분 타임아웃)
- 쿠폰 발급 및 선착순 쿠폰 시스템
- 외부 연동 로깅
- 통일된 API 응답 형식

---

## 2. 공통 응답 형식

### 성공 응답

```json
{
  "success": true,
  "data": { ... },
  "message": null
}
```


### 페이징 응답

```json
{
  "success": true,
  "data": {
    "content": [ ... ],
    "page": 0,
    "size": 20,
    "totalElements": 150,
    "totalPages": 8
  },
  "message": null
}
```


### 에러 응답

```json
{
  "success": false,
  "data": null,
  "message": "에러 메시지",
  "errorCode": "STOCK_INSUFFICIENT"
}
```


### HTTP 상태 코드

| 코드 | 설명 |
| :-- | :-- |
| 200 | 요청 성공 |
| 201 | 리소스 생성 성공 |
| 204 | 성공 (응답 본문 없음) |
| 400 | 잘못된 요청 |
| 404 | 리소스를 찾을 수 없음 |
| 409 | 충돌 (예: 쿠폰 소진, 주문 타임아웃) |
| 500 | 서버 내부 오류 |


---

## 3. 도메인별 주요 API

### 3.1 상품 (Product)

#### 상품 목록 조회

`GET /api/product`

**Query Parameters**

- `page`: 페이지 번호 (기본값: 0)
- `size`: 페이지 크기 (기본값: 20, 최대: 100)
- `sort`: 정렬 기준 (latest, sales, price_asc, price_desc)
- `category`: 카테고리 필터

**Response**

```json
{
  "success": true,
  "data": {
    "content": [
      {
        "productId": 1,
        "productName": "상품명",
        "category": "카테고리",
        "isSoldOut": false
      }
    ],
    "page": 0,
    "size": 20,
    "totalElements": 150,
    "totalPages": 8
  }
}
```

*`isSoldOut`: 모든 옵션이 품절일 경우 true*

#### 상품 상세 조회

`GET /api/product/{productId}`

**Response**

```json
{
  "success": true,
  "data": {
    "productId": 1,
    "productName": "상품명",
    "isSoldOut": false,
    "options": [
      {
        "productOptionId": 1,
        "optionName": "사이즈-M",
        "priceAdjustment": 10000,
        "isSoldOut": false
      }
    ]
  }
}
```


#### 상품 등록

`POST /api/product`

```json
{
  "productName": "상품명",
  "category": "카테고리",
  "description": "설명",
  "imageUrl": "이미지 URL",
  "isExposed": true
}
```


#### 인기 상품 조회

`GET /api/product/top?size=5`

*최근 3일간 판매량 기준 Top 5*

***

### 3.2 재고 (Stock)

#### 재고 조회

`GET /api/stock/{productOptionId}`

**Response**

```json
{
  "success": true,
  "data": {
    "productOptionId": 1,
    "physicalStock": 100,
    "reservedStock": 10,
    "availableStock": 90,
    "isSoldOut": false
  }
}
```

*`availableStock = physicalStock - reservedStock`*

#### 재고 변경

`POST /api/stock/{productOptionId}`

```json
{
  "amount": 10,
  "updatedBy": 1
}
```

*amount: 양수(추가), 음수(감소)*

#### 재고 예약 (주문 생성 시 자동 호출)

`POST /api/stock/reserve`

```json
{
  "orderId": 1,
  "productOptionId": 1,
  "quantity": 2
}
```

**Response**

```json
{
  "success": true,
  "data": {
    "stockReservationId": 1,
    "reservationStatus": "RESERVED",
    "reservedAt": "2025-11-02T12:00:00",
    "expiresAt": "2025-11-02T12:15:00"
  }
}
```

**재고 예약 상태**

- `RESERVED`: 예약중 (15분 유효)
- `CONFIRMED`: 확정됨 (결제 완료 시)
- `RELEASED`: 해제됨 (타임아웃 또는 취소 시)


#### 재고 예약 확정 (결제 완료 시)

`POST /api/stock/reserve/{reservationId}/confirm`

#### 재고 예약 해제 (주문 취소/타임아웃 시)

`POST /api/stock/reserve/{reservationId}/release`

#### 만료된 재고 예약 조회 (배치용)

`GET /api/stock/reserve/expired`

***

### 3.3 유저 (User)

#### 유저 목록 조회

`GET /api/user?role=CUSTOMER`

#### 유저 등록

`POST /api/user`

```json
{
  "username": "사용자명",
  "role": "CUSTOMER"
}
```


#### 유저 포인트 잔액 조회

`GET /api/user/point/{userId}`

***

### 3.4 포인트 (Point)

#### 포인트 충전

`POST /api/point/charge/{userId}`

```json
{
  "amount": 10000.00,
  "description": "충전 사유"
}
```

*최소 충전 금액: 1,000원*

#### 포인트 사용

`POST /api/point/use/{userId}`

```json
{
  "amount": 5000.00,
  "orderId": 1,
  "description": "주문 결제"
}
```


#### 포인트 히스토리 조회

`GET /api/point/{userId}?transactionType=CHARGE`

*transactionType: CHARGE(충전), USE(사용)*

***

### 3.5 장바구니 (Cart)

#### 장바구니 조회

`GET /api/cart/{userId}`

**Response**

```json
{
  "success": true,
  "data": [
    {
      "cartId": 1,
      "productName": "상품명",
      "optionName": "사이즈-M",
      "priceAdjustment": 10000,
      "quantity": 2,
      "subtotal": 20000
    }
  ]
}
```


#### 장바구니 추가

`POST /api/cart/{userId}`

```json
{
  "productOptionId": 1,
  "quantity": 2
}
```

*동일 옵션 추가 시 수량 합산*

#### 장바구니 수정

`PATCH /api/cart/{cartId}`

#### 장바구니 삭제

`DELETE /api/cart/{userId}/{productId}`

***

### 3.6 주문 (Order)

#### 주문 생성

`POST /api/order/{userId}`

```json
{
  "items": [
    {
      "productOptionId": 1,
      "quantity": 2
    }
  ],
  "couponId": 1
}
```

**Response**

```json
{
  "success": true,
  "data": {
    "orderId": 1,
    "orderNumber": "ORD20251102001",
    "orderStatus": "PENDING",
    "totalAmount": 20000,
    "discountAmount": 2000,
    "finalAmount": 13000,
    "expiresAt": "2025-11-02T12:15:00"
  }
}
```

*주문 생성 시 재고 자동 예약 (15분 유효)*

#### 주문 목록 조회

`GET /api/order/{userId}?status=PAID`

**주문 상태**

- `PENDING`: 결제 대기 (재고 예약 상태, 15분 유효)
- `PAID`: 결제 완료 (재고 확정 차감, 포인트/쿠폰 차감 완료)
- `CANCELLED`: 주문 취소 (예약 재고 복원, 포인트/쿠폰 환원)


#### 주문 상세 조회

`GET /api/order/detail/{orderId}`

**Response**

```json
{
  "success": true,
  "data": {
    "orderId": 1,
    "orderStatus": "PAID",
    "items": [
      {
        "orderItemId": 1,
        "productName": "상품명",
        "quantity": 2,
        "itemStatus": "PREPARING"
      }
    ]
  }
}
```


#### 주문 상태 변경

`PATCH /api/order/status/{userId}`

```json
{
  "orderId": 1,
  "orderStatus": "PAID"
}
```


#### 주문 항목 상태 변경

`PATCH /api/order/orderItem/status/{orderItemId}`

```json
{
  "itemStatus": "SHIPPING"
}
```

**주문 항목 상태**

- `PREPARING`: 상품 준비 중 (결제 완료 후 출고 전)
- `SHIPPING`: 배송 중 (물류사 인계 완료)
- `DELIVERED`: 배송 완료 (고객 수령 완료)
- `CANCELLED`: 개별 상품 취소 (PREPARING 상태에서만 가능)


#### 결제

`POST /api/payment/{orderId}/`

```json
{
  "paymentMethod": "CREDIT"
}
```

결제 수단
- `CREDIT`, `CHECK`, `CASH`, `KAKAO`


***

### 3.7 쿠폰 (Coupon)

#### 쿠폰 생성

`POST /api/coupons`

```json
{
  "couponName": "신규가입 쿠폰",
  "discountType": "FIXED",
  "discountValue": 5000.00,
  "minOrderAmount": 30000.00,
  "maxIssueCount": 1000,
  "validFrom": "2025-11-01T00:00:00",
  "validTo": "2025-12-31T23:59:59",
  "createdBy": 1
}
```

**할인 타입**

- `FIXED`: 정액 할인
- `PERCENTAGE`: 정률 할인


#### 쿠폰 목록 조회

`GET /api/coupons?discountType=FIXED`

#### 유저 쿠폰 조회

`GET /api/coupons/user/{userId}?status=ACTIVE`

응답 예시

```json
[
  {
    "userCouponId": 10,
    "couponId": 3,
    "couponName": "10% 할인",
    "discountType": "PERCENTAGE",
    "discountValue": 10.0,
    "status": "ACTIVE",
    "isUsed": false,
    "issuedAt": "2025-11-11T10:00:00"
  }
]
```

#### 쿠폰 발행

`POST /api/coupons/user/{userId}/{couponId}`

#### 선착순 쿠폰 발행

`PATCH /api/coupons/{couponId}/issue`

```json
{
  "userId": 1
}
```


***

### 3.8 외부연동 (Integration)

#### 주문 연동 로그 조회

`GET /api/integrations/logs/{orderId}?integrationType=LOGISTICS`

**연동 타입**

- `LOGISTICS`: 물류 시스템
- `SALES_MANAGEMENT`: 매출 관리 시스템
- `ERP`: 전사적 자원 관리 시스템


#### 실패 건 재시도

`POST /api/integrations/retry/{logId}`

*최대 5회까지 재시도 (1분, 5분, 15분, 30분, 60분 간격)*

---

## 4. 주문-결제 프로세스

![주문-결제 프로세스 시퀀스 다이어그램](order_sequence_diagram.drawio.png)

### 프로세스 흐름

```
1. 장바구니 주문 요청
   ↓
2. 재고 예약 (15분 타임아웃)
   ├─ 재고 부족 → 주문 실패
   └─ 재고 있음 → 예약 완료
   ↓
3. 주문 생성 (status: PENDING)
   ↓
4. 결제 요청
   ├─ 포인트 부족 → 재고 예약 해제 → 실패 응답
   └─ 잔액 충분 → 포인트 차감
   ↓
5. 재고 예약 확정 (실제 차감)
   ↓
6. 주문 상태 업데이트 (status: PAID)
   ↓
7. 외부 시스템 연동 (물류/매출관리)
   ↓
8. 결제 결과 응답
```

### 타임아웃 처리

- **15분 내 미결제**: 주문 자동 취소 (CANCELLED) + 재고 예약 자동 해제
- **배치 작업**: 만료된 예약 조회 API로 주기적 정리

---

## 5. 에러 코드

### 상품 (P)

- `P001`: 상품을 찾을 수 없음
- `P002`: 재고 부족
- `P003`: 노출되지 않은 상품
- `P004`: 상품 옵션을 찾을 수 없음
- `P005`: 상품 옵션 품절


### 재고 (S)

- `S001`: 재고 정보를 찾을 수 없음
- `S002`: 재고 수량 부족
- `S003`: 유효하지 않은 재고 수량
- `S005`: 재고 동시성 충돌


### 유저 (U)

- `U001`: 유저를 찾을 수 없음
- `U002`: 중복된 사용자명
- `U005`: 인증 실패
- `U006`: 권한 없음


### 포인트 (PT)

- `PT001`: 포인트 잔액 부족
- `PT002`: 유효하지 않은 포인트 금액
- `PT004`: 포인트 충전 실패
- `PT007`: 포인트 동시성 오류


### 장바구니 (C)

- `C001`: 장바구니 아이템을 찾을 수 없음
- `C002`: 장바구니가 비어있음
- `C003`: 유효하지 않은 수량
- `C006`: 이미 존재하는 장바구니 아이템


### 주문 (O)

- `O001`: 주문을 찾을 수 없음
- `O003`: 주문 생성 실패
- `O004`: 유효하지 않은 주문 상태
- `O005`: 주문 취소 불가
- `O006`: 결제 실패
- `O009`: 주문 항목이 비어있음


### 쿠폰 (CP)

- `CP001`: 쿠폰을 찾을 수 없음
- `CP002`: 쿠폰 유효기간 만료
- `CP003`: 이미 사용된 쿠폰
- `CP004`: 최소 주문 금액 미달
- `CP005`: 쿠폰 발급 한도 초과
- `CP007`: 이미 발급된 쿠폰
- `CP010`: 선착순 쿠폰 발급 실패 (동시성)


### 외부연동 (I)

- `I001`: 연동 실패
- `I003`: 물류 시스템 연동 실패
- `I004`: 매출 관리 시스템 연동 실패
- `I007`: 최대 재시도 횟수 초과


### 공통 (E)

- `E001`: 잘못된 요청
- `E002`: 인증되지 않음
- `E003`: 접근 거부
- `E004`: 리소스를 찾을 수 없음
- `E500`: 서버 내부 오류
- `E504`: 타임아웃 오류


### ErrorCodes 상수 정의

```java
/**
 * E-Commerce 시스템 에러 코드
 */
public class ErrorCodes {

    // ========== 상품 (P) ==========
    public static final String PRODUCT_NOT_FOUND = "P001";
    public static final String INSUFFICIENT_STOCK = "P002";
    public static final String PRODUCT_NOT_EXPOSED = "P003";
    public static final String PRODUCT_OPTION_NOT_FOUND = "P004";
    public static final String PRODUCT_OPTION_SOLD_OUT = "P005";
    public static final String INVALID_PRODUCT_CATEGORY = "P006";
    public static final String PRODUCT_CREATION_FAILED = "P007";

    // ========== 재고 (S) ==========
    public static final String STOCK_NOT_FOUND = "S001";
    public static final String STOCK_QUANTITY_INSUFFICIENT = "S002";
    public static final String INVALID_STOCK_AMOUNT = "S003";
    public static final String STOCK_UPDATE_UNAUTHORIZED = "S004";
    public static final String STOCK_CONCURRENCY_CONFLICT = "S005";
    public static final String STOCK_RESERVATION_NOT_FOUND = "S006";
    public static final String STOCK_RESERVATION_EXPIRED = "S007";
    public static final String STOCK_RESERVATION_ALREADY_CONFIRMED = "S008";
    public static final String STOCK_RESERVATION_ALREADY_RELEASED = "S009";

    // ========== 유저 (U) ==========
    public static final String USER_NOT_FOUND = "U001";
    public static final String USER_CREATION_FAILED = "U002";
    public static final String USER_GET_LIST_FAILED = "U003";
    public static final String USER_AUTHENTICATION_FAILED = "U004";
    public static final String USER_AUTHORIZATION_FAILED = "U005";

    // ========== 포인트 (PT) ==========
    public static final String INSUFFICIENT_POINT_BALANCE = "PT001";
    public static final String INVALID_POINT_AMOUNT = "PT002";
    public static final String POINT_HISTORY_NOT_FOUND = "PT003";
    public static final String POINT_CHARGE_FAILED = "PT004";
    public static final String POINT_USE_FAILED = "PT005";
    public static final String POINT_CONCURRENCY_ERROR = "PT006";

    // ========== 장바구니 (C) ==========
    public static final String CART_ITEM_NOT_FOUND = "C001";
    public static final String CART_EMPTY = "C002";
    public static final String INVALID_CART_QUANTITY = "C003";
    public static final String CART_ADD_FAILED = "C004";
    public static final String CART_UPDATE_FAILED = "C005";
    public static final String CART_ITEM_ALREADY_EXISTS = "C006";

    // ========== 주문 (O) ==========
    public static final String ORDER_NOT_FOUND = "O001";
    public static final String ORDER_ITEM_NOT_FOUND = "O002";
    public static final String ORDER_CREATION_FAILED = "O003";
    public static final String INVALID_ORDER_STATUS = "O004";
    public static final String ORDER_CANCEL_NOT_ALLOWED = "O005";
    public static final String PAYMENT_FAILED = "O006";
    public static final String PAYMENT_AMOUNT_MISMATCH = "O007";
    public static final String INVALID_PAYMENT_METHOD = "O008";
    public static final String ORDER_ITEMS_EMPTY = "O009";
    public static final String ORDER_STATUS_UPDATE_FAILED = "O010";
    public static final String ORDER_TIMEOUT = "O011";
    public static final String ORDER_ALREADY_PAID = "O012";
    public static final String ORDER_ALREADY_CANCELLED = "O013";
    public static final String INVALID_ORDER_ITEM_STATUS = "O014";

    // ========== 쿠폰 (CP) ==========
    public static final String COUPON_NOT_FOUND = "CP001";
    public static final String COUPON_EXPIRED = "CP002";
    public static final String COUPON_ALREADY_USED = "CP003";
    public static final String COUPON_MIN_ORDER_NOT_MET = "CP004";
    public static final String COUPON_ISSUE_LIMIT_EXCEEDED = "CP005";
    public static final String USER_COUPON_NOT_FOUND = "CP006";
    public static final String COUPON_ALREADY_ISSUED = "CP007";
    public static final String INVALID_COUPON_DISCOUNT_TYPE = "CP008";
    public static final String COUPON_ISSUE_FAILED = "CP009";
    public static final String COUPON_ISSUE_RACE_FAILED = "CP010";
    public static final String COUPON_NOT_VALID_YET = "CP011";

    // ========== 외부연동 (I) ==========
    public static final String INTEGRATION_FAILED = "I001";
    public static final String INTEGRATION_LOG_NOT_FOUND = "I002";
    public static final String LOGISTICS_INTEGRATION_FAILED = "I003";
    public static final String SALES_MANAGEMENT_INTEGRATION_FAILED = "I004";
    public static final String ERP_INTEGRATION_FAILED = "I005";
    public static final String INTEGRATION_RETRY_FAILED = "I006";
    public static final String INTEGRATION_MAX_RETRY_EXCEEDED = "I007";
    public static final String INVALID_INTEGRATION_TYPE = "I008";

    // ========== 공통 (E) ==========
    public static final String BAD_REQUEST = "E001";
    public static final String UNAUTHORIZED = "E002";
    public static final String FORBIDDEN = "E003";
    public static final String NOT_FOUND = "E004";
    public static final String INTERNAL_SERVER_ERROR = "E500";
    public static final String DATABASE_ERROR = "E501";
    public static final String EXTERNAL_API_ERROR = "E502";
    public static final String VALIDATION_FAILED = "E503";
    public static final String TIMEOUT_ERROR = "E504";
}
```

---

**문서 작성일**: 2025-11-02 | **버전**: 1.1.0
