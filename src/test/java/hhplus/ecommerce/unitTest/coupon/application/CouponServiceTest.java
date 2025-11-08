package hhplus.ecommerce.unitTest.coupon.application;

import hhplus.ecommerce.common.domain.exception.CouponException;
import hhplus.ecommerce.coupon.application.service.CouponService;
import hhplus.ecommerce.coupon.domain.model.Coupon;
import hhplus.ecommerce.coupon.domain.model.DiscountType;
import hhplus.ecommerce.coupon.domain.repository.CouponRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CouponServiceTest {

    @Mock
    private CouponRepository couponRepository;

    @InjectMocks
    private CouponService couponService;

    private LocalDateTime validFrom;
    private LocalDateTime validTo;

    @BeforeEach
    void setUp() {
        validFrom = LocalDateTime.now().minusDays(1);
        validTo = LocalDateTime.now().plusDays(30);
    }

    @Test
    @DisplayName("정액 할인 쿠폰을 정상적으로 생성할 수 있다")
    void createFixedCoupon() {
        // given
        when(couponRepository.save(any(Coupon.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // when
        Coupon result = couponService.createCoupon(
                "5000원 할인 쿠폰",
                DiscountType.FIXED,
                BigDecimal.valueOf(5000),
                BigDecimal.valueOf(10000),
                100,
                validFrom,
                validTo,
                1L
        );

        // then
        assertThat(result).isNotNull();
        assertThat(result.getCouponName()).isEqualTo("5000원 할인 쿠폰");
        assertThat(result.getDiscountType()).isEqualTo(DiscountType.FIXED);
        verify(couponRepository, times(1)).save(any(Coupon.class));
    }

    @Test
    @DisplayName("정률 할인 쿠폰을 정상적으로 생성할 수 있다")
    void createPercentageCoupon() {
        // given
        when(couponRepository.save(any(Coupon.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // when
        Coupon result = couponService.createCoupon(
                "10% 할인 쿠폰",
                DiscountType.PERCENTAGE,
                BigDecimal.valueOf(10),
                BigDecimal.valueOf(5000),
                100,
                validFrom,
                validTo,
                1L
        );

        // then
        assertThat(result).isNotNull();
        assertThat(result.getDiscountType()).isEqualTo(DiscountType.PERCENTAGE);
        verify(couponRepository, times(1)).save(any(Coupon.class));
    }

    @Test
    @DisplayName("유효 종료일이 시작일보다 이전이면 예외가 발생한다")
    void createCouponWithInvalidPeriod() {
        // given
        LocalDateTime invalidValidTo = validFrom.minusDays(1);

        // when & then
        assertThatThrownBy(() -> couponService.createCoupon(
                "테스트 쿠폰",
                DiscountType.FIXED,
                BigDecimal.valueOf(5000),
                BigDecimal.valueOf(10000),
                100,
                validFrom,
                invalidValidTo,
                1L
        )).isInstanceOf(CouponException.class)
          .hasMessageContaining("유효 종료일이 시작일보다 이전일 수 없습니다.");

        verify(couponRepository, never()).save(any());
    }

    @Test
    @DisplayName("정률 할인이 100%를 초과하면 예외가 발생한다")
    void createPercentageCouponOver100() {
        // when & then
        assertThatThrownBy(() -> couponService.createCoupon(
                "101% 할인 쿠폰",
                DiscountType.PERCENTAGE,
                BigDecimal.valueOf(101),
                BigDecimal.valueOf(5000),
                100,
                validFrom,
                validTo,
                1L
        )).isInstanceOf(CouponException.class)
          .hasMessageContaining("정률 할인은 100% 이하여야 합니다.");

        verify(couponRepository, never()).save(any());
    }

    @Test
    @DisplayName("쿠폰 목록을 조회할 수 있다")
    void getCoupons() {
        // given
        List<Coupon> expectedCoupons = List.of(
                Coupon.create("쿠폰1", DiscountType.FIXED, BigDecimal.valueOf(5000),
                        BigDecimal.valueOf(10000), 100, validFrom, validTo, 1L),
                Coupon.create("쿠폰2", DiscountType.PERCENTAGE, BigDecimal.valueOf(10),
                        BigDecimal.valueOf(5000), 50, validFrom, validTo, 1L)
        );

        when(couponRepository.findAll()).thenReturn(expectedCoupons);

        // when
        List<Coupon> result = couponService.getCoupons(null);

        // then
        assertThat(result).hasSize(2);
        verify(couponRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("쿠폰을 ID로 조회할 수 있다")
    void getCouponById() {
        // given
        Coupon expectedCoupon = Coupon.create("테스트 쿠폰", DiscountType.FIXED,
                BigDecimal.valueOf(5000), BigDecimal.valueOf(10000), 100, validFrom, validTo, 1L);

        when(couponRepository.findById(1L)).thenReturn(Optional.of(expectedCoupon));

        // when
        Coupon result = couponService.getCouponById(1L);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getCouponName()).isEqualTo("테스트 쿠폰");
        verify(couponRepository, times(1)).findById(1L);
    }

    @Test
    @DisplayName("존재하지 않는 쿠폰 ID로 조회 시 예외가 발생한다")
    void getCouponByIdNotFound() {
        // given
        when(couponRepository.findById(999L)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> couponService.getCouponById(999L))
                .isInstanceOf(CouponException.class);

        verify(couponRepository, times(1)).findById(999L);
    }

    @Test
    @DisplayName("쿠폰 사용 가능 여부를 확인할 수 있다 - 사용 가능")
    void isCouponAvailable_True() {
        // given
        Coupon coupon = Coupon.create("테스트 쿠폰", DiscountType.FIXED,
                BigDecimal.valueOf(5000), BigDecimal.valueOf(10000), 100, validFrom, validTo, 1L);

        // when
        boolean result = couponService.isCouponAvailable(coupon, BigDecimal.valueOf(15000), LocalDateTime.now());

        // then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("쿠폰 사용 가능 여부를 확인할 수 있다 - 최소 금액 미만")
    void isCouponAvailable_BelowMinAmount() {
        // given
        Coupon coupon = Coupon.create("테스트 쿠폰", DiscountType.FIXED,
                BigDecimal.valueOf(5000), BigDecimal.valueOf(10000), 100, validFrom, validTo, 1L);

        // when
        boolean result = couponService.isCouponAvailable(coupon, BigDecimal.valueOf(5000), LocalDateTime.now());

        // then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("쿠폰 사용 가능 여부를 확인할 수 있다 - 유효기간 만료")
    void isCouponAvailable_Expired() {
        // given
        LocalDateTime expiredValidTo = LocalDateTime.now().minusDays(1);
        Coupon coupon = Coupon.create("테스트 쿠폰", DiscountType.FIXED,
                BigDecimal.valueOf(5000), BigDecimal.valueOf(10000), 100,
                LocalDateTime.now().minusDays(30), expiredValidTo, 1L);

        // when
        boolean result = couponService.isCouponAvailable(coupon, BigDecimal.valueOf(15000), LocalDateTime.now());

        // then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("정액 할인 금액을 계산할 수 있다")
    void calculateDiscountAmount_Fixed() {
        // given
        Coupon coupon = Coupon.create("5000원 쿠폰", DiscountType.FIXED,
                BigDecimal.valueOf(5000), BigDecimal.valueOf(10000), 100, validFrom, validTo, 1L);

        // when
        Long result = couponService.calculateDiscountAmount(coupon, BigDecimal.valueOf(20000));

        // then
        assertThat(result).isEqualTo(5000L);
    }

    @Test
    @DisplayName("정률 할인 금액을 계산할 수 있다")
    void calculateDiscountAmount_Percentage() {
        // given
        Coupon coupon = Coupon.create("10% 쿠폰", DiscountType.PERCENTAGE,
                BigDecimal.valueOf(10), BigDecimal.valueOf(5000), 100, validFrom, validTo, 1L);

        // when - 20000원의 10% = 2000원
        Long result = couponService.calculateDiscountAmount(coupon, BigDecimal.valueOf(20000));

        // then
        assertThat(result).isEqualTo(2000L);
    }
}
