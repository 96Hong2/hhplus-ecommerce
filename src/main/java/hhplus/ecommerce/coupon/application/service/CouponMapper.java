package hhplus.ecommerce.coupon.application.service;

import hhplus.ecommerce.coupon.domain.model.Coupon;
import hhplus.ecommerce.coupon.domain.model.UserCoupon;
import hhplus.ecommerce.coupon.presentation.dto.response.CouponResponse;
import hhplus.ecommerce.coupon.presentation.dto.response.UserCouponResponse;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class CouponMapper {

    private final UserCouponService userCouponService;
    private final CouponService couponService;

    public CouponMapper(UserCouponService userCouponService, CouponService couponService) {
        this.userCouponService = userCouponService;
        this.couponService = couponService;
    }

    public CouponResponse toCouponResponse(Coupon coupon) {
        Integer currentIssueCount = userCouponService.getCurrentIssueCount(coupon.getCouponId());
        boolean isAvailable = currentIssueCount < coupon.getMaxIssueCount()
                && LocalDateTime.now().isBefore(coupon.getValidTo())
                && LocalDateTime.now().isAfter(coupon.getValidFrom());

        return new CouponResponse(
                coupon.getCouponId(),
                coupon.getCouponName(),
                coupon.getDiscountType(),
                coupon.getDiscountValue(),
                coupon.getMinOrderAmount(),
                coupon.getMaxIssueCount(),
                currentIssueCount,
                coupon.getValidFrom(),
                coupon.getValidTo(),
                isAvailable
        );
    }

    public List<CouponResponse> toCouponResponseList(List<Coupon> coupons) {
        return coupons.stream()
                .map(this::toCouponResponse)
                .toList();
    }

    public UserCouponResponse toUserCouponResponse(UserCoupon userCoupon, Coupon coupon) {
        return new UserCouponResponse(
                userCoupon.getUserCouponId(),
                coupon.getCouponId(),
                coupon.getCouponName(),
                coupon.getDiscountType(),
                coupon.getDiscountValue(),
                coupon.getMinOrderAmount(),
                userCoupon.getStatus(),
                userCoupon.getUsedAt(),
                userCoupon.getIssuedAt(),
                coupon.getValidFrom(),
                coupon.getValidTo()
        );
    }

    public List<UserCouponResponse> toUserCouponResponseList(List<UserCoupon> userCoupons) {
        return userCoupons.stream()
                .map(userCoupon -> {
                    Coupon coupon = couponService.getCouponById(userCoupon.getCouponId());
                    return toUserCouponResponse(userCoupon, coupon);
                })
                .toList();
    }
}
