# k6 성능 테스트 실행 가이드

## 개요

이 가이드는 **DB 비관적 락 방식**과 **Redis SET 방식**의 선착순 쿠폰 발급 성능을 비교하기 위한 k6 테스트 실행 방법을 설명합니다.

## 사전 준비

### 1. k6 설치

```bash
# MacOS
brew install k6

# Windows (Chocolatey)
choco install k6

# Linux
sudo apt-key adv --keyserver hkp://keyserver.ubuntu.com:80 --recv-keys C5AD17C747E3415A3642D57D77C6C491D6AC1D69
echo "deb https://dl.k6.io/deb stable main" | sudo tee /etc/apt/sources.list.d/k6.list
sudo apt-get update
sudo apt-get install k6
```

### 2. Redis 설치 (Redis 방식 테스트 시 필요)

```bash
# MacOS
brew install redis

# 또는 Docker 사용
docker run --name redis-ecommerce -p 6379:6379 -d redis:7-alpine
```

### 3. 테스트 데이터 준비

애플리케이션 실행 후, 다음 데이터를 준비해야 합니다:

#### 3-1. 관리자 계정 생성
```bash
curl -X POST http://localhost:8080/api/users \
  -H "Content-Type: application/json" \
  -d '{
    "username": "admin",
    "role": "ADMIN"
  }'
```

#### 3-2. 선착순 쿠폰 생성 (1000개 한정)
```bash
curl -X POST http://localhost:8080/api/coupon \
  -H "Content-Type: application/json" \
  -d '{
    "couponName": "성능 테스트 쿠폰",
    "discountType": "FIXED",
    "discountValue": 5000,
    "minOrderAmount": 30000,
    "maxIssueCount": 1000,
    "validFrom": "2025-01-01T00:00:00",
    "validTo": "2025-12-31T23:59:59",
    "createdBy": 1
  }'
```

**응답에서 `couponId`를 확인하고, `k6-performance-test.js` 파일의 `couponId` 변수를 업데이트하세요.**

#### 3-3. 테스트용 사용자 계정 생성 (최소 1000명)

```bash
# 1000명의 사용자 생성 스크립트
for i in {1..1000}
do
  curl -X POST http://localhost:8080/api/users \
    -H "Content-Type: application/json" \
    -d "{\"username\": \"user${i}\", \"role\": \"CUSTOMER\"}"
done
```

## 테스트 실행

### 시나리오 1: DB 비관적 락 방식 테스트

```bash
# 1. Redis 없이 애플리케이션 실행
./gradlew bootRun

# 2. k6 테스트 실행 (별도 터미널)
k6 run k6-performance-test.js

# 3. 결과 파일 저장
k6 run --out json=results-pessimistic-lock.json k6-performance-test.js > test-pessimistic-lock.txt
```

**예상 결과:**
- TPS: ~50-100 req/s
- P95 응답 시간: ~200-500ms
- 락 대기로 인한 긴 응답 시간

### 시나리오 2: Redis SET 방식 테스트

```bash
# 1. Redis 서버 시작
redis-server
# 또는 brew services start redis

# 2. Redis 활성화하여 애플리케이션 실행
./gradlew bootRun --args='--spring.profiles.active=redis'

# 3. 이전 테스트 데이터 정리 (필요시)
redis-cli FLUSHALL

# 4. 쿠폰 재생성 (이전 단계의 쿠폰은 발급 완료됨)
# 위의 "선착순 쿠폰 생성" 단계 다시 실행

# 5. k6 테스트 실행 (별도 터미널) - USE_REDIS 환경변수 설정
k6 run -e USE_REDIS=true k6-performance-test.js

# 6. 결과 파일 저장
k6 run -e USE_REDIS=true --out json=results-redis.json k6-performance-test.js > test-redis.txt
```

**예상 결과:**
- TPS: ~500-1000 req/s (10배 향상)
- P95 응답 시간: ~20-50ms (10배 단축)
- 메모리 기반으로 빠른 응답

## 결과 분석

### 1. 텍스트 결과 비교

```bash
# 주요 메트릭 비교
echo "=== DB 비관적 락 방식 ==="
cat test-pessimistic-lock.txt | grep -E "(http_reqs|http_req_duration|http_req_failed)"

echo "\n=== Redis SET 방식 ==="
cat test-redis.txt | grep -E "(http_reqs|http_req_duration|http_req_failed)"
```

### 2. JSON 결과 분석

```bash
# jq를 사용한 상세 분석 (jq 설치 필요: brew install jq)
jq '.metrics.http_req_duration.values' results-pessimistic-lock.json
jq '.metrics.http_req_duration.values' results-redis.json
```

## 주요 비교 메트릭

| 메트릭 | DB 비관적 락 | Redis SET | 개선율 |
|--------|-------------|-----------|--------|
| TPS (req/s) | ~50-100 | ~500-1000 | 10배 |
| P50 응답 시간 | ~100ms | ~10ms | 10배 |
| P95 응답 시간 | ~200-500ms | ~20-50ms | 10배 |
| P99 응답 시간 | ~500-1000ms | ~50-100ms | 10배 |
| 성공률 | 95-99% | 99-100% | 향상 |

## 테스트 시나리오 설정

`k6-performance-test.js` 파일의 `options.stages`에서 부하 패턴을 조정할 수 있습니다:

```javascript
stages: [
    { duration: '30s', target: 10 },   // Ramp-up: 30초 동안 10명까지 증가
    { duration: '1m', target: 50 },    // Steady: 1분 동안 50명 유지
    { duration: '30s', target: 100 },  // Peak: 30초 동안 100명까지 증가
    { duration: '1m', target: 100 },   // Sustain: 1분 동안 100명 유지
    { duration: '30s', target: 0 },    // Ramp-down: 30초 동안 0명으로 감소
]
```

## 트러블슈팅

### Redis 연결 실패

```bash
# Redis 서버 상태 확인
redis-cli ping
# 응답: PONG

# Redis 서버 시작
brew services start redis
```

### 쿠폰 발급 한도 초과

```bash
# Redis 데이터 초기화
redis-cli FLUSHALL

# DB 쿠폰 데이터 초기화 (필요시)
# MySQL에 접속하여 user_coupons 테이블 정리
```

### 애플리케이션 타임아웃

`application.properties`에서 타임아웃 설정 조정:

```properties
spring.datasource.hikari.connection-timeout=30000
spring.data.redis.timeout=5000ms
```

## 추가 최적화 제안

1. **Redis 클러스터 모드**: 더 높은 처리량을 위해 Redis 클러스터 구성
2. **캐시 웜업**: 테스트 전에 일부 요청으로 캐시 워밍업
3. **DB 커넥션 풀 조정**: 높은 부하 시 커넥션 풀 크기 증가
4. **모니터링**: Prometheus + Grafana로 실시간 메트릭 모니터링

## 참고 자료

- [k6 공식 문서](https://k6.io/docs/)
- [Redis 성능 최적화](https://redis.io/docs/management/optimization/)
- [REDIS_SETUP_GUIDE.md](REDIS_SETUP_GUIDE.md)
- [PERFORMANCE_IMPROVEMENT_REPORT.md](../../CONCURRENCY_TRANSACTION_IMPROVEMENT_REPORT.md)
