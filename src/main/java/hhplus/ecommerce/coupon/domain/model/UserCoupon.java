package hhplus.ecommerce.coupon.domain.model;

import hhplus.ecommerce.common.domain.exception.CouponException;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicLong;

@Getter
public class UserCoupon {

    private static AtomicLong sequence = new AtomicLong(1);

    private final Long userCouponId;
    private final Long userId;
    private final Long couponId;
    private boolean isUsed;
    private LocalDateTime usedAt;
    private Long orderId;
    private final LocalDateTime issuedAt;

    private UserCoupon(Long userCouponId, Long userId, Long couponId, boolean isUsed,
                       LocalDateTime usedAt, Long orderId, LocalDateTime issuedAt) {
        this.userCouponId = userCouponId;
        this.userId = userId;
        this.couponId = couponId;
        this.isUsed = isUsed;
        this.usedAt = usedAt;
        this.orderId = orderId;
        this.issuedAt = issuedAt;
    }

    /**
     * 사용자 쿠폰을 생성한다. (쿠폰 발급)
     * @param userId 사용자 ID
     * @param couponId 쿠폰 ID
     * @return 생성된 사용자 쿠폰
     */
    public static UserCoupon create(Long userId, Long couponId) {
        validateUserId(userId);
        validateCouponId(couponId);

        Long id = sequence.getAndIncrement();
        return new UserCoupon(id, userId, couponId, false, null, null, LocalDateTime.now());
    }

    /**
     * 쿠폰을 사용한다.
     * @param orderId 주문 ID
     */
    public void use(Long orderId) {
        if (this.isUsed) {
            throw CouponException.couponAlreadyUsed(this.userCouponId);
        }

        validateOrderId(orderId);

        this.isUsed = true;
        this.usedAt = LocalDateTime.now();
        this.orderId = orderId;
    }

    /**
     * 쿠폰을 사용 가능한지 확인한다.
     * @return 사용 가능 여부
     */
    public boolean canUse() {
        return !this.isUsed;
    }

    private static void validateUserId(Long userId) {
        if (userId == null) {
            throw CouponException.couponIssueFailed("사용자 ID는 필수입니다.");
        }
    }

    private static void validateCouponId(Long couponId) {
        if (couponId == null) {
            throw CouponException.couponIssueFailed("쿠폰 ID는 필수입니다.");
        }
    }

    private static void validateOrderId(Long orderId) {
        if (orderId == null) {
            throw CouponException.couponIssueFailed("주문 ID는 필수입니다.");
        }
    }
}
