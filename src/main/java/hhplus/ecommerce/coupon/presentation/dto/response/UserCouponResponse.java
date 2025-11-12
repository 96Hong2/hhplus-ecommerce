package hhplus.ecommerce.coupon.presentation.dto.response;

import hhplus.ecommerce.coupon.domain.model.DiscountType;
import hhplus.ecommerce.coupon.domain.model.UserCouponStatus;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@RequiredArgsConstructor
public class UserCouponResponse {
    private final Long userCouponId;
    private final Long couponId;
    private final String couponName;
    private final DiscountType discountType;
    private final BigDecimal discountValue;
    private final BigDecimal minOrderAmount;
    private final UserCouponStatus status;
    private final LocalDateTime usedAt;
    private final LocalDateTime issuedAt;
    private final LocalDateTime validFrom;
    private final LocalDateTime validTo;

}

