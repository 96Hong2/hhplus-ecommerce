package hhplus.ecommerce.coupon.domain.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import hhplus.ecommerce.coupon.domain.model.Coupon;
import hhplus.ecommerce.coupon.domain.model.UserCoupon;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 쿠폰 발급 완료 이벤트
 * 불변 객체로 설계하여 이벤트 신뢰성 확보
 */
@Getter
public class CouponIssuedEvent {

    private final Long userCouponId;
    private final Long userId;
    private final Long couponId;
    private final String couponName;
    private final LocalDateTime issuedAt;

    @JsonCreator
    private CouponIssuedEvent(
            @JsonProperty("userCouponId") Long userCouponId,
            @JsonProperty("userId") Long userId,
            @JsonProperty("couponId") Long couponId,
            @JsonProperty("couponName") String couponName,
            @JsonProperty("issuedAt") LocalDateTime issuedAt) {
        this.userCouponId = userCouponId;
        this.userId = userId;
        this.couponId = couponId;
        this.couponName = couponName;
        this.issuedAt = issuedAt;
    }

    public static CouponIssuedEvent of(UserCoupon userCoupon, Coupon coupon) {
        return new CouponIssuedEvent(
                userCoupon.getUserCouponId(),
                userCoupon.getUserId(),
                userCoupon.getCouponId(),
                coupon.getCouponName(),
                userCoupon.getIssuedAt()
        );
    }
}
