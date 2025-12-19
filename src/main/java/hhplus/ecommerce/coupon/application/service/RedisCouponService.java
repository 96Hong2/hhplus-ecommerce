package hhplus.ecommerce.coupon.application.service;

import hhplus.ecommerce.common.domain.exception.CouponException;
import hhplus.ecommerce.common.event.EventPublisher;
import hhplus.ecommerce.coupon.domain.event.CouponIssuedEvent;
import hhplus.ecommerce.coupon.domain.model.Coupon;
import hhplus.ecommerce.coupon.domain.model.UserCoupon;
import hhplus.ecommerce.coupon.domain.repository.UserCouponRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RedissonClient;
import org.redisson.api.RLock;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Redis SET을 이용한 선착순 쿠폰 발급 서비스
 *
 * 장점:
 * - 메모리 기반으로 빠름 (DB 비관적 락 대비 10배 이상 빠름)
 * - 분산 환경에서도 동작 (멀티 서버 지원)
 * - Redis의 원자적 연산(SADD) 보장
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RedisCouponService {

    private final RedisTemplate<String, String> redisTemplate;
    private final CouponService couponService;
    private final RedissonClient redissonClient;
    private final AsyncUserCouponSaver asyncUserCouponSaver; // SET 방식에서만 사용
    private final UserCouponRepository userCouponRepository;
    private final EventPublisher eventPublisher; // 이벤트 발행

    private static final String COUPON_ISSUE_KEY_PREFIX = "coupon:issue:";
    private static final String COUPON_ISSUE_END_KEY_PREFIX = "coupon:issue:end:";
    private static final String COUPON_LOCK_KEY_PREFIX = "coupon:lock:";

    /**
     * Redis SortedSet를 이용한 선착순 쿠폰 발급 (Lua 스크립트 버전)
     * - Lua 스크립트로 중복 체크 + ZADD + ZCARD 원자적 처리하여 순서 보장
     * - TimeStamp 정렬을 통해 정확한 선착순 확인
     * - 중복 발급 자동 제거 (ZSet 특성)
     */
    public UserCoupon issueCouponWithRedisZset(Long userId, Long couponId) {
        // 1. 종료 여부 캐싱을 통해 1차 필터링
        String endKey = COUPON_ISSUE_END_KEY_PREFIX + couponId;
        String isEnd = redisTemplate.opsForValue().get(endKey);
        if (isEnd != null) {
            throw CouponException.couponIssueLimitExceeded(couponId);
        }

        Coupon coupon = couponService.getCouponById(couponId);
        if (coupon == null) {
            throw CouponException.couponNotFound(couponId);
        }

        String issueKey = COUPON_ISSUE_KEY_PREFIX + couponId;

        // 2. Lua 스크립트로 원자적 처리: 중복 체크 + ZADD + ZCARD(전체 수량 확인)
        String luaScript =
            "local exists = redis.call('ZRANK', KEYS[1], ARGV[2]) " +
            "if exists ~= false then " +
            "    return -1 " + // 이미 발급받음 (중복)
            "end " +
            "redis.call('ZADD', KEYS[1], ARGV[1], ARGV[2]) " +
            "local count = redis.call('ZCARD', KEYS[1]) " +
            "if count > tonumber(ARGV[3]) then " +
            "    redis.call('ZREM', KEYS[1], ARGV[2]) " + // 한도 초과 시 즉시 제거
            "    return 0 " + // 한도 초과
            "end " +
            "return count"; // 발급 후 전체 수량 반환

        Long result = redisTemplate.execute(
            new DefaultRedisScript<>(luaScript, Long.class),
            List.of(issueKey),
            String.valueOf(System.currentTimeMillis()),
            userId.toString(),
            String.valueOf(coupon.getMaxIssueCount())
        );

        // 3. 결과 검증
        if (result == null) {
            throw CouponException.couponIssueFailed("쿠폰 발급 처리 중 오류가 발생했습니다.");
        }

        if (result == -1) {
            throw CouponException.couponAlreadyIssued(userId, couponId);
        }

        // 4. 선착순 한도 초과 체크
        if (result == 0) {
            // 종료 플래그 설정 (2분 캐싱)
            redisTemplate.opsForValue().set(endKey, "true", 2, TimeUnit.MINUTES);
            throw CouponException.couponIssueLimitExceeded(couponId);
        }

        // 5. DB에 저장 (동기 처리 - 즉시 결과 반환)
        UserCoupon userCoupon = UserCoupon.create(userId, couponId);
        try {
            UserCoupon saved = userCouponRepository.save(userCoupon);

            // 쿠폰 발급 이벤트 발행
            eventPublisher.publish(CouponIssuedEvent.of(saved, coupon));

            return saved;
        } catch (Exception e) {
            // DB 저장 실패 시 보상: ZSet에서 제거
            redisTemplate.opsForZSet().remove(issueKey, userId.toString());
            log.error("쿠폰 발급 DB 저장 실패. userId={}, couponId={}", userId, couponId, e);
            throw CouponException.couponIssueFailed("쿠폰 발급 중 오류가 발생했습니다.");
        }
    }

    /**
     * Redis SET을 이용한 선착순 쿠폰 발급
     *
     * 1. Redisson 분산 락 획득
     * 2. Redis SET에 userId 추가 (원자적 연산 - SADD)
     * 3. SET 크기 확인하여 발급 한도 체크
     * 4. DB에 저장 (비동기 작업, 실패 시 Redis에서 제거)
     * 5. 락 해제
     */
    public UserCoupon issueCouponWithRedisSet(Long userId, Long couponId) {
        String key = COUPON_ISSUE_KEY_PREFIX + couponId;
        String lockKey = COUPON_LOCK_KEY_PREFIX + couponId;

        RLock lock = redissonClient.getLock(lockKey);
        try {
            // 1. 분산 락 획득 시도 (대량 동시 요청 처리를 위해 대기 시간 증가)
            if(!lock.tryLock(10, 10, TimeUnit.SECONDS)) {
                throw CouponException.couponIssueFailed("쿠폰 발급이 요청이 너무 많습니다. 잠시 후 다시 시도해주세요.");
            }

            // 2. Redis SET에 userId 추가 (SADD, 동일 유저 중복 발급 체크)
            Long result = redisTemplate.opsForSet().add(key, userId.toString());

            if (result == null || result == 0) {
                throw CouponException.couponAlreadyIssued(userId, couponId);
            }

            // 3. 선착순 한도 체크 (SCARD)
            Long issuedCount = redisTemplate.opsForSet().size(key);
            Coupon coupon = couponService.getCouponById(couponId);
            if (issuedCount == null) {
                issuedCount = 0L;
            }

            if (coupon != null && issuedCount > coupon.getMaxIssueCount()) {
                // 한도 초과 시 SET에서 제거 (보상 처리)
                redisTemplate.opsForSet().remove(key, userId.toString());
                throw CouponException.couponIssueLimitExceeded(couponId);
            }

            // 4. DB에 유저-쿠폰 발급 저장 (비동기 처리 - 락 점유 시간 최소화)
            asyncUserCouponSaver.saveUserCouponAsync(userId, couponId);

            // 비동기 저장 전 임시 객체 반환 (실제 저장은 백그라운드 진행)
            return UserCoupon.create(userId, couponId);

        } catch (Exception e) {
            throw CouponException.couponIssueFailed(e.getMessage());
        } finally {
            lock.unlock();
        }
    }

    // Rua Script 사용하여 선착순 쿠폰 발급 (사용안함, 참고용)
    public UserCoupon issueCouponWithLua(Long userId, Long couponId) {
        Coupon coupon = couponService.getCouponById(couponId);
        String key = COUPON_ISSUE_KEY_PREFIX + couponId;

        // Lua Script (Redis 서버에서 원자적으로 실행)
        String luaScript =
            "if redis.call('SISMEMBER', KEYS[1], ARGV[1]) == 1 then " +
                "    return -1 " +  // 이미 발급받음
                "end " +
                "local count = redis.call('SCARD', KEYS[1]) " +
                "if count >= tonumber(ARGV[2]) then " +
                "    return 0 " +   // 한도 초과
                "end " +
                "redis.call('SADD', KEYS[1], ARGV[1]) " +
                "return 1";        // 발급 성공

        long result = redisTemplate.execute(
            new DefaultRedisScript<>(luaScript, Long.class),
            List.of(key),
            userId.toString(),
            String.valueOf(coupon.getMaxIssueCount())
        );

        if (result == -1) {
            throw CouponException.couponAlreadyIssued(userId, couponId);
        } else if (result == 0) {
            throw CouponException.couponIssueLimitExceeded(couponId);
        }

        // DB 저장
        UserCoupon userCoupon = UserCoupon.create(userId, couponId);
        return userCouponRepository.save(userCoupon);
    }

    /**
     * Redis ZSet 기반 발급된 쿠폰 수 조회
     */
    public Long getIssuedCount(Long couponId) {
        String key = COUPON_ISSUE_KEY_PREFIX + couponId;
        Long size = redisTemplate.opsForZSet().size(key);
        return size != null ? size : 0L;
    }

    /**
     * Redis ZSet 기반 중복 발급 체크
     */
    public boolean isAlreadyIssued(Long userId, Long couponId) {
        String key = COUPON_ISSUE_KEY_PREFIX + couponId;
        Double score = redisTemplate.opsForZSet().score(key, userId.toString());
        return score != null;
    }

    /**
     * Redis 데이터 초기화 (테스트용)
     */
    public void clearCouponIssueData(Long couponId) {
        String key = COUPON_ISSUE_KEY_PREFIX + couponId;
        redisTemplate.delete(key);
    }
}
