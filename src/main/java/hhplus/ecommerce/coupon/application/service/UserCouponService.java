package hhplus.ecommerce.coupon.application.service;

import hhplus.ecommerce.common.domain.exception.CouponException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import hhplus.ecommerce.coupon.domain.model.Coupon;
import hhplus.ecommerce.coupon.domain.model.UserCoupon;
import hhplus.ecommerce.coupon.domain.repository.UserCouponRepository;
import hhplus.ecommerce.coupon.domain.model.UserCouponStatus;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserCouponService {
    private final UserCouponRepository userCouponRepository;
    private final CouponService couponService;

    /**
     * 일반 쿠폰 발급
     */
    public UserCoupon issueCoupon(Long userId, Long couponId) {
        Coupon coupon = couponService.getCouponById(couponId);

        // 중복 발급 체크
        if (userCouponRepository.findByUserIdAndCouponId(userId, couponId).isPresent()) {
            throw CouponException.couponAlreadyIssued(userId, couponId);
        }

        // 발급 수 체크
        Integer currentCount = userCouponRepository.countByCouponId(couponId);
        if (currentCount >= coupon.getMaxIssueCount()) {
            throw CouponException.couponIssueLimitExceeded(couponId);
        }

        UserCoupon userCoupon = UserCoupon.create(
                userId,
                couponId
        );

        return userCouponRepository.save(userCoupon);
    }

    /**
     * 선착순 쿠폰 발급 (동시성 제어)
     */
    public UserCoupon issueFirstComeCoupon(Long userId, Long couponId) {
        Coupon coupon = couponService.getCouponById(couponId);

        // 중복 발급 체크
        if (userCouponRepository.findByUserIdAndCouponId(userId, couponId).isPresent()) {
            throw CouponException.couponAlreadyIssued(userId, couponId);
        }

        // AtomicInteger CAS 방식으로 발급 수 증가 (동시성 제어)
        boolean issued = userCouponRepository.incrementIssueCountIfAvailable(
                couponId,
                coupon.getMaxIssueCount()
        );

        if (!issued) {
            throw CouponException.couponIssueLimitExceeded(couponId);
        }

        UserCoupon userCoupon = UserCoupon.create(
                userId,
                couponId
        );

        return userCouponRepository.save(userCoupon);
    }

    /**
     * 사용자 쿠폰 목록 조회
     */
    public List<UserCoupon> getUserCoupons(Long userId, UserCouponStatus status) {
        if (status == null) {
            return userCouponRepository.findByUserId(userId);
        }
        return userCouponRepository.findByUserIdAndStatus(userId, status);
    }

    /**
     * 쿠폰 사용 처리
     */
    public UserCoupon useCoupon(Long userCouponId, Long orderId) {
        UserCoupon userCoupon = userCouponRepository.findById(userCouponId)
                .orElseThrow(() -> CouponException.couponNotFound(userCouponId));

        if (userCoupon.getStatus() == UserCouponStatus.USED) {
            throw CouponException.couponAlreadyUsed(userCouponId);
        }

        userCoupon.use(orderId);
        return userCouponRepository.save(userCoupon);
    }

    /**
     * 특정 쿠폰 현재 발급 수 조회
     */
    public Integer getCurrentIssueCount(Long couponId) {
        return userCouponRepository.countByCouponId(couponId);
    }
}
