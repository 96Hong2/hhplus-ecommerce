package hhplus.ecommerce.coupon.domain.model;

import hhplus.ecommerce.common.domain.exception.CouponException;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_coupons", indexes = {
    @Index(name = "idx_user_coupon", columnList = "user_id, coupon_id", unique = true),
    @Index(name = "idx_user_status_issued", columnList = "user_id, status, issued_at"),
    @Index(name = "idx_coupon_id", columnList = "coupon_id"),
    @Index(name = "idx_status_issued", columnList = "status, issued_at")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserCoupon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long userCouponId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "coupon_id", nullable = false)
    private Long couponId;

    @Column(name = "used_at")
    private LocalDateTime usedAt;

    @Column(name = "order_id")
    private Long orderId;

    @CreationTimestamp
    @Column(name = "issued_at", nullable = false, updatable = false)
    private LocalDateTime issuedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private UserCouponStatus status;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    private UserCoupon(Long userCouponId, Long userId, Long couponId,
                       LocalDateTime usedAt, Long orderId, UserCouponStatus status) {
        this.userCouponId = userCouponId;
        this.userId = userId;
        this.couponId = couponId;
        this.usedAt = usedAt;
        this.orderId = orderId;
        this.status = status;
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

        return new UserCoupon(null, userId, couponId, null, null, UserCouponStatus.ACTIVE);
    }

    /**
     * 쿠폰을 사용한다.
     * @param orderId 주문 ID
     */
    public void use(Long orderId) {
        if (this.status == UserCouponStatus.USED) {
            throw CouponException.couponAlreadyUsed(this.userCouponId);
        }

        validateOrderId(orderId);

        this.usedAt = LocalDateTime.now();
        this.orderId = orderId;
        this.status = UserCouponStatus.USED;
    }

    /**
     * 쿠폰을 사용 가능한지 확인한다.
     * @return 사용 가능 여부
     */
    public boolean canUse() {
        return this.status == UserCouponStatus.ACTIVE;
    }

    /**
     * 쿠폰을 만료 처리한다.
     */
    public void expire() {
        if (this.status == UserCouponStatus.USED) {
            return;
        }
        this.status = UserCouponStatus.EXPIRED;
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
