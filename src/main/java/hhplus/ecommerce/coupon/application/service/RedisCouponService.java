package hhplus.ecommerce.coupon.application.service;

import hhplus.ecommerce.common.domain.exception.CouponException;
import hhplus.ecommerce.coupon.domain.model.Coupon;
import hhplus.ecommerce.coupon.domain.model.UserCoupon;
import hhplus.ecommerce.coupon.domain.repository.UserCouponRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    private final UserCouponRepository userCouponRepository;

    private static final String COUPON_ISSUE_KEY_PREFIX = "coupon:issue:";

    /**
     * Redis SET을 이용한 선착순 쿠폰 발급
     *
     * 1. Redis SET에 userId 추가 (원자적 연산 - SADD)
     * 2. SET 크기 확인하여 발급 한도 체크
     * 3. DB에 저장 (실패 시 Redis에서 제거)
     */
    @Transactional
    public UserCoupon issueCouponWithRedis(Long userId, Long couponId) {
        String key = COUPON_ISSUE_KEY_PREFIX + couponId;

        // 1. Redis SET에 userId 추가 (원자적 연산)
        // SADD는 이미 존재하면 0을 반환, 새로 추가되면 1을 반환
        Long result = redisTemplate.opsForSet().add(key, userId.toString());

        if (result == null || result == 0) {
            throw CouponException.couponAlreadyIssued(userId, couponId);
        }

        // 2. SET 크기 확인 (발급 한도 체크)
        Long count = redisTemplate.opsForSet().size(key);
        Coupon coupon = couponService.getCouponById(couponId);

        if (count != null && count > coupon.getMaxIssueCount()) {
            // 한도 초과 시 SET에서 제거 (보상 처리)
            redisTemplate.opsForSet().remove(key, userId.toString());
            throw CouponException.couponIssueLimitExceeded(couponId);
        }

        // 3. DB에 저장 (비동기로 처리 가능하지만 여기서는 동기 처리)
        try {
            UserCoupon userCoupon = UserCoupon.create(userId, couponId);
            return userCouponRepository.save(userCoupon);
        } catch (Exception e) {
            // 실패 시 Redis에서 제거 (보상 처리)
            redisTemplate.opsForSet().remove(key, userId.toString());
            log.error("쿠폰 발급 DB 저장 실패. userId={}, couponId={}", userId, couponId, e);
            throw e;
        }
    }

    /**
     * Redis 기반 발급된 쿠폰 수 조회
     */
    public Long getIssuedCount(Long couponId) {
        String key = COUPON_ISSUE_KEY_PREFIX + couponId;
        Long size = redisTemplate.opsForSet().size(key);
        return size != null ? size : 0L;
    }

    /**
     * Redis 기반 중복 발급 체크
     */
    public boolean isAlreadyIssued(Long userId, Long couponId) {
        String key = COUPON_ISSUE_KEY_PREFIX + couponId;
        return Boolean.TRUE.equals(
            redisTemplate.opsForSet().isMember(key, userId.toString())
        );
    }

    /**
     * Redis 데이터 초기화 (테스트용)
     */
    public void clearCouponIssueData(Long couponId) {
        String key = COUPON_ISSUE_KEY_PREFIX + couponId;
        redisTemplate.delete(key);
    }
}
