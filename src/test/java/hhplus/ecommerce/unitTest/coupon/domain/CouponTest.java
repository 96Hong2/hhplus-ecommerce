package hhplus.ecommerce.unitTest.coupon.domain;

import hhplus.ecommerce.common.domain.exception.CouponException;
import hhplus.ecommerce.coupon.domain.model.Coupon;
import hhplus.ecommerce.coupon.domain.model.DiscountType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class CouponTest {

    @Test
    @DisplayName("정액 할인 쿠폰을 정상적으로 생성할 수 있다.")
    void createFixedCoupon() {
        // given
        LocalDateTime validFrom = LocalDateTime.now();
        LocalDateTime validTo = validFrom.plusDays(30);

        // when
        Coupon coupon = Coupon.create("신규가입 쿠폰", DiscountType.FIXED, BigDecimal.valueOf(5000),
                BigDecimal.valueOf(30000), 1000, validFrom, validTo, 1L);

        // then
        assertThat(coupon.getCouponName()).isEqualTo("신규가입 쿠폰");
        assertThat(coupon.getDiscountType()).isEqualTo(DiscountType.FIXED);
        assertThat(coupon.getDiscountValue()).isEqualByComparingTo(BigDecimal.valueOf(5000));
        assertThat(coupon.getMinOrderAmount()).isEqualByComparingTo(BigDecimal.valueOf(30000));
        assertThat(coupon.getMaxIssueCount()).isEqualTo(1000);
        assertThat(coupon.getIssuedCount()).isZero();
        assertThat(coupon.getCreatedBy()).isEqualTo(1L);
    }

    @Test
    @DisplayName("정률 할인 쿠폰을 정상적으로 생성할 수 있다.")
    void createPercentageCoupon() {
        // given
        LocalDateTime validFrom = LocalDateTime.now();
        LocalDateTime validTo = validFrom.plusDays(30);

        // when
        Coupon coupon = Coupon.create("10% 할인 쿠폰", DiscountType.PERCENTAGE, BigDecimal.valueOf(10),
                BigDecimal.valueOf(50000), 500, validFrom, validTo, 1L);

        // then
        assertThat(coupon.getDiscountType()).isEqualTo(DiscountType.PERCENTAGE);
        assertThat(coupon.getDiscountValue()).isEqualByComparingTo(BigDecimal.valueOf(10));
    }

    @Test
    @DisplayName("쿠폰 생성 시 쿠폰명이 null이면 예외가 발생한다.")
    void createCouponWithNullName() {
        // given
        LocalDateTime validFrom = LocalDateTime.now();
        LocalDateTime validTo = validFrom.plusDays(30);

        // when & then
        assertThatThrownBy(() -> Coupon.create(null, DiscountType.FIXED, BigDecimal.valueOf(5000),
                BigDecimal.valueOf(30000), 1000, validFrom, validTo, 1L))
                .isInstanceOf(CouponException.class)
                .hasMessageContaining("쿠폰명은 필수입니다.");
    }

    @Test
    @DisplayName("쿠폰 생성 시 할인 타입이 null이면 예외가 발생한다.")
    void createCouponWithNullDiscountType() {
        // given
        LocalDateTime validFrom = LocalDateTime.now();
        LocalDateTime validTo = validFrom.plusDays(30);

        // when & then
        assertThatThrownBy(() -> Coupon.create("쿠폰명", null, BigDecimal.valueOf(5000),
                BigDecimal.valueOf(30000), 1000, validFrom, validTo, 1L))
                .isInstanceOf(CouponException.class)
                .hasMessageContaining("유효하지 않은 할인 타입입니다.");
    }

    @Test
    @DisplayName("쿠폰 생성 시 할인 값이 음수면 예외가 발생한다.")
    void createCouponWithNegativeDiscountValue() {
        // given
        LocalDateTime validFrom = LocalDateTime.now();
        LocalDateTime validTo = validFrom.plusDays(30);

        // when & then
        assertThatThrownBy(() -> Coupon.create("쿠폰명", DiscountType.FIXED, BigDecimal.valueOf(-1000),
                BigDecimal.valueOf(30000), 1000, validFrom, validTo, 1L))
                .isInstanceOf(CouponException.class)
                .hasMessageContaining("할인 값은 0 이상이어야 합니다.");
    }

    @Test
    @DisplayName("정률 할인 쿠폰 생성 시 할인율이 100%를 초과하면 예외가 발생한다.")
    void createPercentageCouponWithOverHundredPercent() {
        // given
        LocalDateTime validFrom = LocalDateTime.now();
        LocalDateTime validTo = validFrom.plusDays(30);

        // when & then
        assertThatThrownBy(() -> Coupon.create("쿠폰명", DiscountType.PERCENTAGE, BigDecimal.valueOf(101),
                BigDecimal.valueOf(30000), 1000, validFrom, validTo, 1L))
                .isInstanceOf(CouponException.class)
                .hasMessageContaining("정률 할인은 100%를 초과할 수 없습니다.");
    }

    @Test
    @DisplayName("쿠폰 생성 시 최소 주문 금액이 음수면 예외가 발생한다.")
    void createCouponWithNegativeMinOrderAmount() {
        // given
        LocalDateTime validFrom = LocalDateTime.now();
        LocalDateTime validTo = validFrom.plusDays(30);

        // when & then
        assertThatThrownBy(() -> Coupon.create("쿠폰명", DiscountType.FIXED, BigDecimal.valueOf(5000),
                BigDecimal.valueOf(-1000), 1000, validFrom, validTo, 1L))
                .isInstanceOf(CouponException.class)
                .hasMessageContaining("최소 주문 금액은 0원 이상이어야 합니다.");
    }

    @Test
    @DisplayName("쿠폰 생성 시 최대 발급 수량이 0 이하면 예외가 발생한다.")
    void createCouponWithInvalidMaxIssueCount() {
        // given
        LocalDateTime validFrom = LocalDateTime.now();
        LocalDateTime validTo = validFrom.plusDays(30);

        // when & then
        assertThatThrownBy(() -> Coupon.create("쿠폰명", DiscountType.FIXED, BigDecimal.valueOf(5000),
                BigDecimal.valueOf(30000), 0, validFrom, validTo, 1L))
                .isInstanceOf(CouponException.class)
                .hasMessageContaining("최대 발급 수량은 1 이상이어야 합니다.");
    }

    @Test
    @DisplayName("쿠폰 생성 시 유효기간 시작일이 종료일보다 늦으면 예외가 발생한다.")
    void createCouponWithInvalidValidPeriod() {
        // given
        LocalDateTime validFrom = LocalDateTime.now();
        LocalDateTime validTo = validFrom.minusDays(1);

        // when & then
        assertThatThrownBy(() -> Coupon.create("쿠폰명", DiscountType.FIXED, BigDecimal.valueOf(5000),
                BigDecimal.valueOf(30000), 1000, validFrom, validTo, 1L))
                .isInstanceOf(CouponException.class)
                .hasMessageContaining("유효기간 시작일은 종료일보다 이전이어야 합니다.");
    }

    @Test
    @DisplayName("쿠폰을 정상적으로 발급할 수 있다.")
    void issueCoupon() {
        // given
        LocalDateTime validFrom = LocalDateTime.now();
        LocalDateTime validTo = validFrom.plusDays(30);
        Coupon coupon = Coupon.create("쿠폰명", DiscountType.FIXED, BigDecimal.valueOf(5000),
                BigDecimal.valueOf(30000), 10, validFrom, validTo, 1L);

        // when
        boolean result = coupon.issue();

        // then
        assertThat(result).isTrue();
        assertThat(coupon.getIssuedCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("쿠폰 발급 시 최대 발급 수량을 초과하면 예외가 발생한다.")
    void issueCouponOverMaxCount() {
        // given
        LocalDateTime validFrom = LocalDateTime.now();
        LocalDateTime validTo = validFrom.plusDays(30);
        Coupon coupon = Coupon.create("쿠폰명", DiscountType.FIXED, BigDecimal.valueOf(5000),
                BigDecimal.valueOf(30000), 2, validFrom, validTo, 1L);
        coupon.issue();
        coupon.issue();

        // when & then
        assertThatThrownBy(coupon::issue)
                .isInstanceOf(CouponException.class)
                .hasMessageContaining("쿠폰 발급 수량이 초과되었습니다.");
    }

    @Test
    @DisplayName("쿠폰이 유효기간 내에 있는지 확인할 수 있다.")
    void isValid() {
        // given
        LocalDateTime validFrom = LocalDateTime.now().minusDays(1);
        LocalDateTime validTo = LocalDateTime.now().plusDays(30);
        Coupon coupon = Coupon.create("쿠폰명", DiscountType.FIXED, BigDecimal.valueOf(5000),
                BigDecimal.valueOf(30000), 1000, validFrom, validTo, 1L);

        // when & then
        assertThat(coupon.isValid()).isTrue();
        assertThat(coupon.isExpired()).isFalse();
        assertThat(coupon.isNotValidYet()).isFalse();
    }

    @Test
    @DisplayName("쿠폰이 만료되었는지 확인할 수 있다.")
    void isExpired() {
        // given
        LocalDateTime validFrom = LocalDateTime.now().minusDays(30);
        LocalDateTime validTo = LocalDateTime.now().minusDays(1);
        Coupon coupon = Coupon.create("쿠폰명", DiscountType.FIXED, BigDecimal.valueOf(5000),
                BigDecimal.valueOf(30000), 1000, validFrom, validTo, 1L);

        // when & then
        assertThat(coupon.isExpired()).isTrue();
        assertThat(coupon.isValid()).isFalse();
    }

    @Test
    @DisplayName("쿠폰이 아직 유효하지 않은지 확인할 수 있다.")
    void isNotValidYet() {
        // given
        LocalDateTime validFrom = LocalDateTime.now().plusDays(1);
        LocalDateTime validTo = LocalDateTime.now().plusDays(30);
        Coupon coupon = Coupon.create("쿠폰명", DiscountType.FIXED, BigDecimal.valueOf(5000),
                BigDecimal.valueOf(30000), 1000, validFrom, validTo, 1L);

        // when & then
        assertThat(coupon.isNotValidYet()).isTrue();
        assertThat(coupon.isValid()).isFalse();
    }

    @Test
    @DisplayName("정액 할인 쿠폰의 할인 금액을 계산할 수 있다.")
    void calculateFixedDiscount() {
        // given
        LocalDateTime validFrom = LocalDateTime.now().minusDays(1);
        LocalDateTime validTo = LocalDateTime.now().plusDays(30);
        Coupon coupon = Coupon.create("쿠폰명", DiscountType.FIXED, BigDecimal.valueOf(5000),
                BigDecimal.valueOf(30000), 1000, validFrom, validTo, 1L);

        // when
        BigDecimal discount = coupon.calculateDiscount(BigDecimal.valueOf(50000));

        // then
        assertThat(discount).isEqualByComparingTo(BigDecimal.valueOf(5000));
    }

    @Test
    @DisplayName("정률 할인 쿠폰의 할인 금액을 계산할 수 있다.")
    void calculatePercentageDiscount() {
        // given
        LocalDateTime validFrom = LocalDateTime.now().minusDays(1);
        LocalDateTime validTo = LocalDateTime.now().plusDays(30);
        Coupon coupon = Coupon.create("쿠폰명", DiscountType.PERCENTAGE, BigDecimal.valueOf(10),
                BigDecimal.valueOf(30000), 1000, validFrom, validTo, 1L);

        // when
        BigDecimal discount = coupon.calculateDiscount(BigDecimal.valueOf(50000));

        // then - 50000 * 0.1 = 5000
        assertThat(discount).isEqualByComparingTo(BigDecimal.valueOf(5000));
    }

    @Test
    @DisplayName("할인 금액 계산 시 최소 주문 금액 미달이면 예외가 발생한다.")
    void calculateDiscountWithInsufficientOrderAmount() {
        // given
        LocalDateTime validFrom = LocalDateTime.now().minusDays(1);
        LocalDateTime validTo = LocalDateTime.now().plusDays(30);
        Coupon coupon = Coupon.create("쿠폰명", DiscountType.FIXED, BigDecimal.valueOf(5000),
                BigDecimal.valueOf(30000), 1000, validFrom, validTo, 1L);

        // when & then
        assertThatThrownBy(() -> coupon.calculateDiscount(BigDecimal.valueOf(20000)))
                .isInstanceOf(CouponException.class)
                .hasMessageContaining("최소 주문 금액을 충족하지 않습니다.");
    }

    @Test
    @DisplayName("할인 금액 계산 시 쿠폰이 만료되었으면 예외가 발생한다.")
    void calculateDiscountWithExpiredCoupon() {
        // given
        LocalDateTime validFrom = LocalDateTime.now().minusDays(30);
        LocalDateTime validTo = LocalDateTime.now().minusDays(1);
        Coupon coupon = Coupon.create("쿠폰명", DiscountType.FIXED, BigDecimal.valueOf(5000),
                BigDecimal.valueOf(30000), 1000, validFrom, validTo, 1L);

        // when & then
        assertThatThrownBy(() -> coupon.calculateDiscount(BigDecimal.valueOf(50000)))
                .isInstanceOf(CouponException.class)
                .hasMessageContaining("만료된 쿠폰입니다.");
    }

    @Test
    @DisplayName("할인 금액 계산 시 쿠폰이 아직 유효하지 않으면 예외가 발생한다.")
    void calculateDiscountWithNotValidYetCoupon() {
        // given
        LocalDateTime validFrom = LocalDateTime.now().plusDays(1);
        LocalDateTime validTo = LocalDateTime.now().plusDays(30);
        Coupon coupon = Coupon.create("쿠폰명", DiscountType.FIXED, BigDecimal.valueOf(5000),
                BigDecimal.valueOf(30000), 1000, validFrom, validTo, 1L);

        // when & then
        assertThatThrownBy(() -> coupon.calculateDiscount(BigDecimal.valueOf(50000)))
                .isInstanceOf(CouponException.class)
                .hasMessageContaining("아직 사용할 수 없는 쿠폰입니다.");
    }

    @Test
    @DisplayName("쿠폰을 사용할 수 있는지 확인할 수 있다.")
    void canBeUsed() {
        // given
        LocalDateTime validFrom = LocalDateTime.now().minusDays(1);
        LocalDateTime validTo = LocalDateTime.now().plusDays(30);
        Coupon coupon = Coupon.create("쿠폰명", DiscountType.FIXED, BigDecimal.valueOf(5000),
                BigDecimal.valueOf(30000), 1000, validFrom, validTo, 1L);

        // when & then
        assertThat(coupon.canBeUsed(BigDecimal.valueOf(50000))).isTrue();
        assertThat(coupon.canBeUsed(BigDecimal.valueOf(20000))).isFalse();
    }

    @Test
    @DisplayName("쿠폰 발급 가능 여부를 확인할 수 있다.")
    void canIssue() {
        // given
        LocalDateTime validFrom = LocalDateTime.now();
        LocalDateTime validTo = validFrom.plusDays(30);
        Coupon coupon = Coupon.create("쿠폰명", DiscountType.FIXED, BigDecimal.valueOf(5000),
                BigDecimal.valueOf(30000), 2, validFrom, validTo, 1L);

        // when & then
        assertThat(coupon.canIssue()).isTrue();
        coupon.issue();
        assertThat(coupon.canIssue()).isTrue();
        coupon.issue();
        assertThat(coupon.canIssue()).isFalse();
    }
}
