package hhplus.ecommerce.coupon.application.service;

import hhplus.ecommerce.common.domain.exception.CouponException;
import hhplus.ecommerce.coupon.domain.model.Coupon;
import hhplus.ecommerce.coupon.domain.model.UserCoupon;
import hhplus.ecommerce.coupon.domain.repository.UserCouponRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
// 비동기 처리는 프록시를 사용하는데, 같은 클래스의 내부 메서드 호출이면 프록시 호출이 안되어 클래스 분리함
public class AsyncUserCouponSaver {
    private final UserCouponRepository userCouponRepository;
    private final CouponService couponService;
    private final RedisTemplate<String, String> redisTemplate;

    private static final String COUPON_ISSUE_KEY_PREFIX = "coupon:issue:";
    private static final String COUPON_ISSUE_END_KEY_PREFIX = "coupon:issue:end:";
    private static final String COUPON_ISSUED_COUNT_KEY_PREFIX = "coupon:issued:count:";

    // SortedSet 비동기 워커 방식 - RedisCouponService.issueCouponWithRedisZset에서 Lua 스크립트 동기 방식으로 변경됨
    /*
    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public CompletableFuture<UserCoupon> processUserCouponAsync(Long userId, Long couponId) {
        String issueKey = COUPON_ISSUE_KEY_PREFIX + couponId;
        String countKey = COUPON_ISSUED_COUNT_KEY_PREFIX + couponId;
        String endKey = COUPON_ISSUE_END_KEY_PREFIX + couponId;

        // 종료 여부 캐싱을 통해 1차 거름
        String isEnd = redisTemplate.opsForValue().get(COUPON_ISSUE_END_KEY_PREFIX + couponId);
        if (isEnd != null) {
            throw CouponException.couponIssueLimitExceeded(couponId);
        }

        Coupon coupon = couponService.getCouponById(couponId);
        if (coupon == null) {
            throw CouponException.couponNotFound(couponId);
        }

        // 요청에 대한 결과를 하나씩 리턴해야하고, 발급가능수량보다 더 적은 사람이 발급요청을 했을 수 있기 때문에 한꺼번에 가져오지 않음
        // >> 문제: popMin()은 요청한 userId가 아닌 다른 userId를 꺼낼 수 있음
        // Set<ZSetOperations.TypedTuple<String>> zset = redisTemplate.opsForZSet().rangeWithScores(issueKey, 0, coupon.getMaxIssueCount()-1);

        int issuedCount = 0;
        if (redisTemplate.hasKey(countKey)) {
            issuedCount = Integer.parseInt(redisTemplate.opsForValue().get(countKey));
        }

        if (issuedCount <= coupon.getMaxIssueCount()) {
            ZSetOperations.TypedTuple<String> tuple = redisTemplate.opsForZSet().popMin(issueKey);
            try {
                UserCoupon userCoupon = UserCoupon.create(userId, couponId);
                UserCoupon saved = userCouponRepository.save(userCoupon);
                redisTemplate.opsForValue().increment(countKey);
                return CompletableFuture.completedFuture(saved);
            } catch (Exception e) {
                // DB 저장 실패 시 보상 처리 필요
                log.error("쿠폰 발급 DB 저장 실패. userId={}, couponId={}", userId, couponId, e);
                return CompletableFuture.failedFuture(e);
            }
        } else {
            redisTemplate.opsForValue().set(endKey, LocalDate.now().toString(), 2, TimeUnit.MINUTES);
            throw CouponException.couponIssueLimitExceeded(couponId);
        }
    }
    */

    // UserCoupon 테이블(DB)에 저장하는 것을 비동기로 빼서 락/IO를 오래 홀드하지 않도록 함
    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW) // 새로운 트랜잭션에서 실행
    public CompletableFuture<UserCoupon> saveUserCouponAsync(Long userId, Long couponId) {
        String setKey = COUPON_ISSUE_KEY_PREFIX + couponId;

        try {
            UserCoupon userCoupon = UserCoupon.create(userId, couponId);
            UserCoupon saved = userCouponRepository.save(userCoupon);
            return CompletableFuture.completedFuture(saved);
        } catch (Exception e) {
            // DB 저장 실패 시 보상 : Redis에서 제거
            redisTemplate.opsForSet().remove(setKey, userId.toString());
            log.error("쿠폰 발급 DB 저장 실패. userId={}, couponId={}", userId, couponId, e);
            return CompletableFuture.failedFuture(e);
        }
    }
}
