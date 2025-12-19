package hhplus.ecommerce.coupon.domain.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import hhplus.ecommerce.coupon.domain.model.UserCoupon;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 쿠폰 사용 완료 이벤트
 *
 */
@Getter
public class CouponUsedEvent {

    private final Long userCouponId;
    private final Long userId;
    private final Long couponId;
    private final Long orderId;
    private final LocalDateTime usedAt;

    @JsonCreator
    private CouponUsedEvent(
            @JsonProperty("userCouponId") Long userCouponId,
            @JsonProperty("userId") Long userId,
            @JsonProperty("couponId") Long couponId,
            @JsonProperty("orderId") Long orderId,
            @JsonProperty("usedAt") LocalDateTime usedAt) {
        this.userCouponId = userCouponId;
        this.userId = userId;
        this.couponId = couponId;
        this.orderId = orderId;
        this.usedAt = usedAt;
    }

    public static CouponUsedEvent of(UserCoupon userCoupon) {
        return new CouponUsedEvent(
                userCoupon.getUserCouponId(),
                userCoupon.getUserId(),
                userCoupon.getCouponId(),
                userCoupon.getOrderId(),
                userCoupon.getUsedAt()
        );
    }
}
