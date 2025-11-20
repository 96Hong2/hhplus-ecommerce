# Redis를 이용한 선착순 쿠폰 동시성 처리 가이드

## 1. Redis 설치 및 실행

### MacOS
```bash
# Homebrew로 설치
brew install redis

# Redis 서버 시작
redis-server

# 또는 백그라운드 실행
brew services start redis

# 연결 테스트
redis-cli ping
# 응답: PONG
```

### Docker
```bash
docker run --name redis-ecommerce -p 6379:6379 -d redis:7-alpine
```

## 2. Redis 기반 쿠폰 발급 서비스 구현 예시

```java
@Service
@RequiredArgsConstructor
public class RedisCouponService {

    private final RedisTemplate<String, String> redisTemplate;
    private final CouponRepository couponRepository;
    private final UserCouponRepository userCouponRepository;

    /**
     * Redis SET을 이용한 선착순 쿠폰 발급
     *
     * 장점:
     * - 메모리 기반으로 빠름 (DB 비관적 락 대비 10배 이상 빠름)
     * - 분산 환경에서도 동작 (멀티 서버 지원)
     * - Redis의 원자적 연산(SADD) 보장
     */
    public UserCoupon issueCouponWithRedis(Long userId, Long couponId) {
        String key = "coupon:issue:" + couponId;

        // 1. Redis SET에 userId 추가 (원자적 연산)
        Long result = redisTemplate.opsForSet().add(key, userId.toString());

        if (result == null || result == 0) {
            throw CouponException.couponAlreadyIssued(userId, couponId);
        }

        // 2. SET 크기 확인 (발급 한도 체크)
        Long count = redisTemplate.opsForSet().size(key);
        Coupon coupon = couponRepository.findById(couponId).orElseThrow();

        if (count > coupon.getMaxIssueCount()) {
            // 한도 초과 시 SET에서 제거
            redisTemplate.opsForSet().remove(key, userId.toString());
            throw CouponException.couponIssueLimitExceeded(couponId);
        }

        // 3. DB에 저장 (비동기로 처리 가능)
        try {
            UserCoupon userCoupon = UserCoupon.create(userId, couponId);
            return userCouponRepository.save(userCoupon);
        } catch (Exception e) {
            // 실패 시 Redis에서 제거 (보상 처리)
            redisTemplate.opsForSet().remove(key, userId.toString());
            throw e;
        }
    }

    /**
     * 발급된 쿠폰 수 조회
     */
    public Long getIssuedCount(Long couponId) {
        String key = "coupon:issue:" + couponId;
        return redisTemplate.opsForSet().size(key);
    }

    /**
     * 중복 발급 체크
     */
    public boolean isAlreadyIssued(Long userId, Long couponId) {
        String key = "coupon:issue:" + couponId;
        return Boolean.TRUE.equals(
            redisTemplate.opsForSet().isMember(key, userId.toString())
        );
    }
}
```

## 3. 성능 비교 테스트

### 비관적 락 방식 (Before)
```java
@Transactional
public UserCoupon issueCouponWithPessimisticLock(Long userId, Long couponId) {
    Coupon coupon = couponRepository.findByIdWithLock(couponId); // SELECT FOR UPDATE
    coupon.issue();
    couponRepository.save(coupon);

    UserCoupon userCoupon = UserCoupon.create(userId, couponId);
    return userCouponRepository.save(userCoupon);
}
```

**예상 성능**:
- TPS: ~50 req/s
- P95 응답 시간: ~200ms
- 락 대기 시간: 평균 100ms

### Redis SET 방식 (After)
```java
public UserCoupon issueCouponWithRedis(Long userId, Long couponId) {
    // Redis SADD (원자적 연산, 메모리 기반)
    Long result = redisTemplate.opsForSet().add(key, userId.toString());
    // ...
}
```

**예상 성능**:
- TPS: ~500 req/s (10배 향상)
- P95 응답 시간: ~20ms (10배 단축)
- 락 대기 시간: 없음

## 4. k6 성능 테스트 실행

```bash
# k6 설치
brew install k6

# 테스트 실행
k6 run k6-performance-test.js

# 결과 파일 생성
k6 run --out json=results.json k6-performance-test.js

# 특정 시나리오만 실행
k6 run --env SCENARIO=coupon k6-performance-test.js
```

### 테스트 시나리오
1. **Ramp-up**: 30초 동안 0 → 10명 증가
2. **Steady**: 1분 동안 50명 유지
3. **Peak**: 30초 동안 50 → 100명 증가
4. **Sustain**: 1분 동안 100명 유지
5. **Ramp-down**: 30초 동안 100 → 0명 감소

### 비교 방법
```bash
# 1. 비관적 락 방식 테스트
# application.properties에서 Redis 비활성화
k6 run k6-performance-test.js > pessimistic-lock-results.txt

# 2. Redis 방식 테스트
# application.properties에서 Redis 활성화
k6 run k6-performance-test.js > redis-results.txt

# 3. 결과 비교
diff pessimistic-lock-results.txt redis-results.txt
```

## 5. 주의사항

### Redis 장애 대응
```java
@Service
public class FallbackCouponService {

    @Autowired(required = false)
    private RedisCouponService redisCouponService;

    @Autowired
    private UserCouponService userCouponService; // DB 기반

    public UserCoupon issueCoupon(Long userId, Long couponId) {
        try {
            if (redisCouponService != null) {
                return redisCouponService.issueCouponWithRedis(userId, couponId);
            }
        } catch (RedisConnectionException e) {
            log.warn("Redis 연결 실패, DB 방식으로 폴백", e);
        }

        // Fallback: DB 비관적 락 방식
        return userCouponService.issueCouponWithPessimisticLock(userId, couponId);
    }
}
```

### Redis 데이터 동기화
```java
@Scheduled(fixedDelay = 60000) // 1분마다
public void syncRedisToDatabase() {
    // Redis SET 데이터를 DB와 동기화
    // 필요 시 불일치 데이터 정정
}
```

## 6. 테스트 체크리스트

- [ ] Redis 서버 실행 확인
- [ ] application-redis.properties 활성화
- [ ] 쿠폰 데이터 준비 (한도 100명)
- [ ] k6 설치 및 테스트 스크립트 실행
- [ ] 비관적 락 방식 vs Redis 방식 TPS 비교
- [ ] 응답 시간 (P50, P95, P99) 비교
- [ ] 발급 한도 정확성 검증 (정확히 100명만 발급)
- [ ] 중복 발급 없음 검증

---

**참고**: 실제 프로덕션 환경에서는 Redis Cluster 또는 Sentinel을 사용하여 고가용성을 확보해야 합니다.
