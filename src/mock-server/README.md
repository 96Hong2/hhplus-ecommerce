# Mock API Server

이커머스 프로젝트의 Mock API 서버입니다. 개발 및 테스트 용도로 실제 비즈니스 로직을 시뮬레이션합니다.

## 🚀 서버 실행

### 1. 의존성 설치 (최초 1회)

```bash
cd src/mock-server
npm install
```

### 2. 서버 시작

```bash
# 커스텀 비즈니스 로직 포함 서버 (권장)
npm run dev
# 또는
npm start
```

```bash
# 순수 JSON Server만 사용 (단순 CRUD)
npm run simple
```

서버가 시작되면 `http://localhost:3001`에서 접근 가능합니다.

## 📋 구현된 커스텀 API

### 1. 쿠폰 선착순 발급
```http
PATCH /api/coupons/:couponId/issue
Content-Type: application/json

{
  "userId": 1
}
```

**기능:**
- 발급 수량 제한 체크 (`issuedCount` < `maxIssueCount`)
- 중복 발급 방지
- 자동으로 `issuedCount` 증가
- `userCoupons` 테이블에 발급 기록 추가

**응답 예시:**
```json
{
  "success": true,
  "data": {
    "id": "1234567890",
    "userId": 1,
    "couponId": 1,
    "isUsed": false,
    "issuedAt": "2025-01-20T10:00:00Z",
    "couponName": "신규가입 10% 할인",
    "discountType": "PERCENTAGE",
    "discountValue": 10
  },
  "message": "쿠폰이 발급되었습니다"
}
```

**에러 케이스:**
- 쿠폰 소진: `409 COUPON_EXHAUSTED`
- 중복 발급: `400 COUPON_ALREADY_ISSUED`

---

### 2. 포인트 충전
```http
POST /api/point/charge/:userId
Content-Type: application/json

{
  "amount": 10000,
  "description": "포인트 충전"
}
```

**기능:**
- 최소 충전 금액 검증 (1,000원)
- 1,000원 단위 검증
- 자동으로 `users.pointBalance` 업데이트
- `pointHistories` 테이블에 충전 기록 추가

**응답 예시:**
```json
{
  "success": true,
  "data": {
    "id": "1234567890",
    "userId": 1,
    "transactionType": "CHARGE",
    "amount": 10000,
    "balanceAfter": 60000,
    "createdAt": "2025-01-20T10:00:00Z"
  },
  "message": "포인트가 충전되었습니다"
}
```

---

### 3. 주문 생성 (재고 차감 포함)
```http
POST /api/order/:userId
Content-Type: application/json

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

**기능:**
- 재고 확인 (부족 시 주문 실패)
- 재고 자동 차감
- 주문 금액 자동 계산
- 쿠폰 할인 적용 (FIXED, PERCENTAGE)
- 주문 번호 자동 생성 (`ORD-YYYYMMDD-XXXX`)
- `orderItems` 테이블에 주문 상품 추가
- 품절 시 `isSoldOut` 자동 업데이트

**응답 예시:**
```json
{
  "success": true,
  "data": {
    "id": "1234567890",
    "orderNumber": "ORD-20250120-5678",
    "userId": 1,
    "totalAmount": 118000,
    "discountAmount": 11800,
    "finalAmount": 101200,
    "orderStatus": "PENDING",
    "createdAt": "2025-01-20T10:00:00Z"
  },
  "message": "주문이 생성되었습니다"
}
```

**에러 케이스:**
- 재고 부족: `400 STOCK_INSUFFICIENT`

---

### 4. 결제 처리
```http
POST /api/order/:orderId/payment
Content-Type: application/json

{
  "paymentMethod": "POINT"
}
```

**기능:**
- 주문 상태 검증 (`PENDING`만 결제 가능)
- 포인트 잔액 확인 및 차감
- `pointHistories` 테이블에 사용 기록 추가
- 쿠폰 사용 처리 (`isUsed` → true)
- 주문 상태 변경 (`PENDING` → `PAID`)

**응답 예시:**
```json
{
  "success": true,
  "data": {
    "orderId": 1,
    "orderNumber": "ORD-20250120-5678",
    "paymentStatus": "PAID",
    "finalAmount": 101200,
    "paidAt": "2025-01-20T10:05:00Z"
  },
  "message": "결제가 완료되었습니다"
}
```

**에러 케이스:**
- 포인트 부족: `400 POINT_INSUFFICIENT`
- 잘못된 주문 상태: `400 INVALID_ORDER_STATUS`

---

### 5. 장바구니 추가 (중복 시 수량 합산)
```http
POST /api/cart/:userId
Content-Type: application/json

{
  "productOptionId": 1,
  "quantity": 2
}
```

**기능:**
- 중복 상품 확인
- 중복 시 수량 자동 합산 (신규 항목 생성 X)
- 중복 아닐 시 새 장바구니 항목 추가

**응답 예시:**
```json
{
  "success": true,
  "data": {
    "id": "1",
    "userId": 1,
    "productOptionId": 1,
    "quantity": 4,
    "updatedAt": "2025-01-20T10:00:00Z"
  },
  "message": "장바구니 수량이 업데이트되었습니다"
}
```

---

## 📦 기본 CRUD 엔드포인트

모든 리소스는 JSON Server의 기본 RESTful API를 지원합니다:

### 상품
- `GET /api/products` - 상품 목록
- `GET /api/products/:id` - 상품 상세
- `POST /api/products` - 상품 추가
- `PUT /api/products/:id` - 상품 수정
- `DELETE /api/products/:id` - 상품 삭제

### 기타 리소스
- `/api/productOptions` - 상품 옵션
- `/api/users` - 사용자
- `/api/carts` - 장바구니
- `/api/orders` - 주문
- `/api/orderItems` - 주문 항목
- `/api/coupons` - 쿠폰
- `/api/userCoupons` - 사용자 쿠폰
- `/api/pointHistories` - 포인트 이력
- `/api/integrationLogs` - 연동 로그

### 쿼리 파라미터
```bash
# 페이징
GET /api/products?_page=1&_limit=10

# 정렬
GET /api/products?_sort=createdAt&_order=desc

# 필터링
GET /api/carts?userId=1

# 검색
GET /api/products?q=이어폰

# 관계 데이터 포함
GET /api/products?_embed=productOptions
```

## 🔧 구현 방식 선택 이유

### 왜 커스텀 server.js를 사용하나요?

1. **실제 비즈니스 로직 시뮬레이션**
   - 재고 차감, 쿠폰 발급 등 핵심 기능 테스트 가능
   - 프론트엔드 개발 시 실제 API와 유사한 동작 확인

2. **에러 처리 테스트**
   - 재고 부족, 쿠폰 소진, 포인트 부족 등 실제 에러 시나리오 테스트
   - 에러 코드와 메시지 표준화 확인

3. **학습 목적**
   - 비즈니스 로직의 실제 동작 이해
   - 트랜잭션 개념 학습 (재고 차감, 포인트 차감 등)

4. **개발 생산성**
   - Spring Boot API 개발 전 프론트엔드 작업 가능
   - API 스펙 사전 검증

### 대안: 순수 JSON Server

단순 CRUD만 필요하다면:
```bash
npm run simple
```

**장점:** 설정이 필요 없고 빠름
**단점:** 비즈니스 로직 없음, 재고 차감/쿠폰 발급 등 불가능

## 📝 참고사항

### 제약사항
- 트랜잭션 미지원 (실제 DB처럼 롤백 불가)
- 복잡한 동시성 제어 불가
- 인증/인가 기능 없음
- 개발/테스트 용도로만 사용

### 데이터 초기화
`db.json` 파일을 수정하면 서버 재시작 시 반영됩니다.
백업이 필요하면 `db.json` 파일을 복사해두세요.

### 디버깅
브라우저에서 `http://localhost:3001`에 접속하면 전체 리소스 확인 가능합니다.

## 📚 추가 문서
- [JSON Server 가이드](JSON_SERVER_GUIDE.md)
- [API 명세서](../../docs/api/api-specification.md)
- [데이터 모델](../../docs/api/data-models.md)
