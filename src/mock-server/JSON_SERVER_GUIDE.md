# 📦 JSON Server 사용 가이드

## 1️⃣ 설치

```bash
npm install -g json-server
```

## 2️⃣ 서버 실행

```bash
# 기본 실행 (포트 3000)
json-server --watch db.json

# 포트 변경
json-server --watch db.json --port 4000

# CORS 활성화 + 포트 변경
json-server --watch db.json --port 4000 --host 0.0.0.0
```

## 3️⃣ API 엔드포인트

### 📌 기본 CRUD 패턴

| 메서드 | 경로 | 설명 |
|--------|------|------|
| GET | `/products` | 전체 목록 조회 |
| GET | `/products/1` | ID로 단건 조회 |
| POST | `/products` | 새 항목 생성 |
| PUT | `/products/1` | 전체 수정 |
| PATCH | `/products/1` | 부분 수정 |
| DELETE | `/products/1` | 삭제 |

### 🔍 쿼리 파라미터

```bash
# 페이징
GET /products?_page=1&_limit=10

# 정렬 (오름차순)
GET /products?_sort=price&_order=asc

# 필터링
GET /products?category=전자제품

# 검색 (부분 일치)
GET /products?q=이어폰

# 관계 데이터 포함 (_embed)
GET /products?_embed=productOptions

# 부모 데이터 포함 (_expand)
GET /productOptions?_expand=product
```

## 4️⃣ 주요 리소스

### 상품 관련
- `/products` - 상품 목록
- `/productOptions` - 상품 옵션

### 사용자 관련
- `/users` - 사용자
- `/pointHistories` - 포인트 이력
- `/carts` - 장바구니

### 주문 관련
- `/orders` - 주문
- `/orderItems` - 주문 항목

### 쿠폰 관련
- `/coupons` - 쿠폰
- `/userCoupons` - 사용자 쿠폰

### 연동 로그
- `/integrationLogs` - 외부 연동 로그

## 5️⃣ 실전 예제

### 사용자 1의 장바구니 조회
```bash
GET http://localhost:3000/carts?userId=1
```

### 특정 주문의 주문 항목 조회
```bash
GET http://localhost:3000/orderItems?orderId=1
```

### 전자제품 카테고리 상품만 조회
```bash
GET http://localhost:3000/products?category=전자제품
```

### 사용 가능한 쿠폰만 조회
```bash
GET http://localhost:3000/userCoupons?isUsed=false
```

### 새 상품 추가
```bash
POST http://localhost:3000/products
Content-Type: application/json

{
  "productName": "노트북",
  "category": "전자제품",
  "description": "고성능 노트북",
  "imageUrl": "https://example.com/laptop.jpg",
  "exposeFlag": true,
  "createdAt": "2025-01-20T00:00:00Z",
  "updatedAt": "2025-01-20T00:00:00Z"
}
```

## 6️⃣ 팁

💡 **데이터 초기화**: `db.json` 파일을 수정 후 서버 재시작  
💡 **실시간 반영**: `--watch` 옵션으로 파일 변경 자동 감지  
💡 **백업**: 중요 데이터 수정 전 `db.json` 백업 권장  
💡 **디버깅**: 브라우저에서 `http://localhost:3000` 접속하면 전체 리소스 확인 가능

## 7️⃣ 제약사항

⚠️ 실제 인증/인가 기능 없음  
⚠️ 복잡한 비즈니스 로직 처리 불가  
⚠️ 트랜잭션 미지원  
⚠️ 개발/테스트 용도로만 사용 권장
