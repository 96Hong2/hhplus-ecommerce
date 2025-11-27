package hhplus.ecommerce.coupon.application.service;

import hhplus.ecommerce.coupon.domain.model.UserCoupon;
import hhplus.ecommerce.coupon.domain.repository.UserCouponRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
// 비동기 처리는 프록시를 사용하는데, 같은 클래스의 내부 메서드 호출이면 프록시 호출이 안되어 클래스 분리함
public class AsyncUserCouponSaver {
    private final UserCouponRepository userCouponRepository;
    private final RedisTemplate<String, String> redisTemplate;

    private static final String COUPON_ISSUE_KEY_PREFIX = "coupon:issue:";

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
