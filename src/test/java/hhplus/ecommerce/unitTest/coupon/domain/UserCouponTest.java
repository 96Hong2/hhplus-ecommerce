package hhplus.ecommerce.unitTest.coupon.domain;

import hhplus.ecommerce.common.domain.exception.CouponException;
import hhplus.ecommerce.coupon.domain.model.UserCoupon;
import hhplus.ecommerce.coupon.domain.model.UserCouponStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class UserCouponTest {

    @Test
    @DisplayName("사용자 쿠폰을 정상적으로 생성할 수 있다.")
    void createUserCoupon() {
        // when
        UserCoupon userCoupon = UserCoupon.create(1L, 1L);

        // then
        assertThat(userCoupon.getUserId()).isEqualTo(1L);
        assertThat(userCoupon.getCouponId()).isEqualTo(1L);
        assertThat(userCoupon.getStatus()).isEqualTo(UserCouponStatus.ACTIVE);
        assertThat(userCoupon.getUsedAt()).isNull();
        assertThat(userCoupon.getOrderId()).isNull();
    }

    @Test
    @DisplayName("사용자 쿠폰 생성 시 userId가 null이면 예외가 발생한다.")
    void createUserCouponWithNullUserId() {
        // when & then
        assertThatThrownBy(() -> UserCoupon.create(null, 1L))
                .isInstanceOf(CouponException.class)
                .hasMessageContaining("사용자 ID는 필수입니다.");
    }

    @Test
    @DisplayName("사용자 쿠폰 생성 시 couponId가 null이면 예외가 발생한다.")
    void createUserCouponWithNullCouponId() {
        // when & then
        assertThatThrownBy(() -> UserCoupon.create(1L, null))
                .isInstanceOf(CouponException.class)
                .hasMessageContaining("쿠폰 ID는 필수입니다.");
    }

    @Test
    @DisplayName("쿠폰을 정상적으로 사용할 수 있다.")
    void useCoupon() {
        // given
        UserCoupon userCoupon = UserCoupon.create(1L, 1L);

        // when
        userCoupon.use(1L);

        // then
        assertThat(userCoupon.getStatus()).isEqualTo(UserCouponStatus.USED);
        assertThat(userCoupon.getUsedAt()).isNotNull();
        assertThat(userCoupon.getOrderId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("이미 사용된 쿠폰을 재사용하면 예외가 발생한다.")
    void useAlreadyUsedCoupon() {
        // given
        UserCoupon userCoupon = UserCoupon.create(1L, 1L);
        userCoupon.use(1L);

        // when & then
        assertThatThrownBy(() -> userCoupon.use(2L))
                .isInstanceOf(CouponException.class)
                .hasMessageContaining("이미 사용된 쿠폰입니다.");
    }

    @Test
    @DisplayName("쿠폰 사용 시 orderId가 null이면 예외가 발생한다.")
    void useCouponWithNullOrderId() {
        // given
        UserCoupon userCoupon = UserCoupon.create(1L, 1L);

        // when & then
        assertThatThrownBy(() -> userCoupon.use(null))
                .isInstanceOf(CouponException.class)
                .hasMessageContaining("주문 ID는 필수입니다.");
    }

    @Test
    @DisplayName("쿠폰 사용 가능 여부를 확인할 수 있다.")
    void canUse() {
        // given
        UserCoupon userCoupon = UserCoupon.create(1L, 1L);

        // when & then
        assertThat(userCoupon.canUse()).isTrue();

        // 사용 후
        userCoupon.use(1L);
        assertThat(userCoupon.canUse()).isFalse();
    }
}
