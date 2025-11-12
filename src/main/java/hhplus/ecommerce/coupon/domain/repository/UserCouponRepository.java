package hhplus.ecommerce.coupon.domain.repository;

import hhplus.ecommerce.coupon.domain.model.UserCoupon;
import hhplus.ecommerce.coupon.domain.model.UserCouponStatus;

import java.util.List;
import java.util.Optional;

public interface UserCouponRepository {
    UserCoupon save(UserCoupon userCoupon);

    Optional<UserCoupon> findById(Long userCouponId);

    List<UserCoupon> findByUserId(Long userId);

    List<UserCoupon> findByUserIdAndStatus(Long userId, UserCouponStatus status);

    Optional<UserCoupon> findByUserIdAndCouponId(Long userId, Long couponId);

    Integer countByCouponId(Long couponId);

    boolean incrementIssueCountIfAvailable(Long couponId, Integer maxIssueCount);
}
