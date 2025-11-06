package hhplus.ecommerce.coupon.presentation.dto.response;

import hhplus.ecommerce.coupon.domain.model.DiscountType;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@RequiredArgsConstructor
public class CouponResponse {
    private final Long couponId;
    private final String couponName;
    private final DiscountType discountType;
    private final BigDecimal discountValue;
    private final BigDecimal minOrderAmount;
    private final Integer maxIssueCount;
    private final Integer currentIssueCount;
    private final LocalDateTime validFrom;
    private final LocalDateTime validTo;
    private final Boolean isAvailable;
}
