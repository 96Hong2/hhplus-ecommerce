package hhplus.ecommerce.unitTest.coupon.repository;

import hhplus.ecommerce.coupon.domain.model.Coupon;
import hhplus.ecommerce.coupon.domain.model.DiscountType;
import hhplus.ecommerce.coupon.infrastructure.repository.InMemoryCouponRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class CouponRepositoryTest {

    private InMemoryCouponRepository couponRepository;

    @BeforeEach
    void setUp() {
        couponRepository = new InMemoryCouponRepository();
    }

    @Test
    @DisplayName("쿠폰을 저장할 수 있다")
    void saveCoupon() {
        Coupon coupon = Coupon.create(
                "테스트 쿠폰",
                DiscountType.FIXED,
                BigDecimal.valueOf(10000),
                BigDecimal.valueOf(50000),
                100,
                LocalDateTime.now(),
                LocalDateTime.now().plusDays(30),
                1L
        );

        Coupon savedCoupon = couponRepository.save(coupon);

        assertThat(savedCoupon).isNotNull();
        assertThat(savedCoupon.getCouponId()).isNotNull();
        assertThat(savedCoupon.getCouponName()).isEqualTo("테스트 쿠폰");
    }

    @Test
    @DisplayName("ID로 쿠폰을 조회할 수 있다")
    void findById() {
        Coupon coupon = Coupon.create(
                "테스트 쿠폰",
                DiscountType.FIXED,
                BigDecimal.valueOf(10000),
                BigDecimal.valueOf(50000),
                100,
                LocalDateTime.now(),
                LocalDateTime.now().plusDays(30),
                1L
        );
        Coupon savedCoupon = couponRepository.save(coupon);

        Optional<Coupon> foundCoupon = couponRepository.findById(savedCoupon.getCouponId());

        assertThat(foundCoupon).isPresent();
        assertThat(foundCoupon.get().getCouponName()).isEqualTo("테스트 쿠폰");
    }

    @Test
    @DisplayName("전체 쿠폰 목록을 조회할 수 있다")
    void findAll() {
        couponRepository.save(Coupon.create("쿠폰1", DiscountType.FIXED, BigDecimal.valueOf(5000), BigDecimal.ZERO, 50, LocalDateTime.now(), LocalDateTime.now().plusDays(30), 1L));
        couponRepository.save(Coupon.create("쿠폰2", DiscountType.PERCENTAGE, BigDecimal.valueOf(10), BigDecimal.ZERO, 100, LocalDateTime.now(), LocalDateTime.now().plusDays(30), 1L));

        List<Coupon> coupons = couponRepository.findAll();

        assertThat(coupons).hasSize(2);
    }

    @Test
    @DisplayName("할인 타입으로 쿠폰 목록을 조회할 수 있다")
    void findByDiscountType() {
        couponRepository.save(Coupon.create("정액쿠폰1", DiscountType.FIXED, BigDecimal.valueOf(5000), BigDecimal.ZERO, 50, LocalDateTime.now(), LocalDateTime.now().plusDays(30), 1L));
        couponRepository.save(Coupon.create("정액쿠폰2", DiscountType.FIXED, BigDecimal.valueOf(10000), BigDecimal.ZERO, 100, LocalDateTime.now(), LocalDateTime.now().plusDays(30), 1L));
        couponRepository.save(Coupon.create("정률쿠폰", DiscountType.PERCENTAGE, BigDecimal.valueOf(10), BigDecimal.ZERO, 100, LocalDateTime.now(), LocalDateTime.now().plusDays(30), 1L));

        List<Coupon> fixedCoupons = couponRepository.findByDiscountType(DiscountType.FIXED);
        List<Coupon> percentageCoupons = couponRepository.findByDiscountType(DiscountType.PERCENTAGE);

        assertThat(fixedCoupons).hasSize(2);
        assertThat(percentageCoupons).hasSize(1);
    }
}
