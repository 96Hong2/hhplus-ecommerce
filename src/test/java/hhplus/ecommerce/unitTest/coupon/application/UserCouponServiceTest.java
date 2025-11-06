package hhplus.ecommerce.unitTest.coupon.application;

import hhplus.ecommerce.common.domain.exception.CouponException;
import hhplus.ecommerce.coupon.application.service.CouponService;
import hhplus.ecommerce.coupon.application.service.UserCouponService;
import hhplus.ecommerce.coupon.domain.model.Coupon;
import hhplus.ecommerce.coupon.domain.model.DiscountType;
import hhplus.ecommerce.coupon.domain.model.UserCoupon;
import hhplus.ecommerce.coupon.domain.repository.UserCouponRepository;
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
class UserCouponServiceTest {

    @Mock
    private UserCouponRepository userCouponRepository;

    @Mock
    private CouponService couponService;

    @InjectMocks
    private UserCouponService userCouponService;

    private Coupon testCoupon;

    @BeforeEach
    void setUp() {
        testCoupon = Coupon.create(
                "테스트 쿠폰",
                DiscountType.FIXED,
                BigDecimal.valueOf(5000),
                BigDecimal.valueOf(10000),
                100,
                LocalDateTime.now().minusDays(1),
                LocalDateTime.now().plusDays(30),
                1L
        );
    }

    @Test
    @DisplayName("일반 쿠폰을 정상적으로 발급할 수 있다")
    void issueCoupon() {
        // given
        Long userId = 1L;
        Long couponId = 1L;

        when(couponService.getCouponById(couponId)).thenReturn(testCoupon);
        when(userCouponRepository.findByUserIdAndCouponId(userId, couponId))
                .thenReturn(Optional.empty());
        when(userCouponRepository.countByCouponId(couponId)).thenReturn(50);
        when(userCouponRepository.save(any(UserCoupon.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // when
        UserCoupon result = userCouponService.issueCoupon(userId, couponId);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getUserId()).isEqualTo(userId);
        assertThat(result.getCouponId()).isEqualTo(couponId);
        verify(userCouponRepository, times(1)).save(any(UserCoupon.class));
    }

    @Test
    @DisplayName("중복 발급 시도 시 예외가 발생한다")
    void issueCouponDuplicate() {
        // given
        Long userId = 1L;
        Long couponId = 1L;
        UserCoupon existingUserCoupon = UserCoupon.create(userId, couponId);

        when(couponService.getCouponById(couponId)).thenReturn(testCoupon);
        when(userCouponRepository.findByUserIdAndCouponId(userId, couponId))
                .thenReturn(Optional.of(existingUserCoupon));

        // when & then
        assertThatThrownBy(() -> userCouponService.issueCoupon(userId, couponId))
                .isInstanceOf(CouponException.class);

        verify(userCouponRepository, never()).save(any());
    }

    @Test
    @DisplayName("발급 한도 초과 시 예외가 발생한다")
    void issueCouponLimitExceeded() {
        // given
        Long userId = 1L;
        Long couponId = 1L;

        when(couponService.getCouponById(couponId)).thenReturn(testCoupon);
        when(userCouponRepository.findByUserIdAndCouponId(userId, couponId))
                .thenReturn(Optional.empty());
        when(userCouponRepository.countByCouponId(couponId)).thenReturn(100); // 한도 도달

        // when & then
        assertThatThrownBy(() -> userCouponService.issueCoupon(userId, couponId))
                .isInstanceOf(CouponException.class);

        verify(userCouponRepository, never()).save(any());
    }

    @Test
    @DisplayName("선착순 쿠폰을 정상적으로 발급할 수 있다")
    void issueFirstComeCoupon() {
        // given
        Long userId = 1L;
        Long couponId = 1L;

        when(couponService.getCouponById(couponId)).thenReturn(testCoupon);
        when(userCouponRepository.findByUserIdAndCouponId(userId, couponId))
                .thenReturn(Optional.empty());
        when(userCouponRepository.incrementIssueCountIfAvailable(couponId, 100)).thenReturn(true);
        when(userCouponRepository.save(any(UserCoupon.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // when
        UserCoupon result = userCouponService.issueFirstComeCoupon(userId, couponId);

        // then
        assertThat(result).isNotNull();
        verify(userCouponRepository, times(1))
                .incrementIssueCountIfAvailable(couponId, 100);
        verify(userCouponRepository, times(1)).save(any(UserCoupon.class));
    }

    @Test
    @DisplayName("선착순 쿠폰 발급 한도 초과 시 예외가 발생한다")
    void issueFirstComeCouponLimitExceeded() {
        // given
        Long userId = 1L;
        Long couponId = 1L;

        when(couponService.getCouponById(couponId)).thenReturn(testCoupon);
        when(userCouponRepository.findByUserIdAndCouponId(userId, couponId))
                .thenReturn(Optional.empty());
        when(userCouponRepository.incrementIssueCountIfAvailable(couponId, 100)).thenReturn(false);

        // when & then
        assertThatThrownBy(() -> userCouponService.issueFirstComeCoupon(userId, couponId))
                .isInstanceOf(CouponException.class);

        verify(userCouponRepository, never()).save(any());
    }

    @Test
    @DisplayName("사용자 쿠폰 목록을 조회할 수 있다")
    void getUserCoupons() {
        // given
        Long userId = 1L;
        List<UserCoupon> expectedCoupons = List.of(
                UserCoupon.create(userId, 1L),
                UserCoupon.create(userId, 2L)
        );

        when(userCouponRepository.findByUserId(userId)).thenReturn(expectedCoupons);

        // when
        List<UserCoupon> result = userCouponService.getUserCoupons(userId, null);

        // then
        assertThat(result).hasSize(2);
        verify(userCouponRepository, times(1)).findByUserId(userId);
    }

    @Test
    @DisplayName("사용 여부로 필터링하여 쿠폰 목록을 조회할 수 있다")
    void getUserCouponsByUsed() {
        // given
        Long userId = 1L;
        List<UserCoupon> expectedCoupons = List.of(UserCoupon.create(userId, 1L));

        when(userCouponRepository.findByUserIdAndIsUsed(userId, false))
                .thenReturn(expectedCoupons);

        // when
        List<UserCoupon> result = userCouponService.getUserCoupons(userId, false);

        // then
        assertThat(result).hasSize(1);
        verify(userCouponRepository, times(1)).findByUserIdAndIsUsed(userId, false);
    }

    @Test
    @DisplayName("쿠폰을 사용할 수 있다")
    void useCoupon() {
        // given
        Long userCouponId = 1L;
        Long orderId = 100L;
        UserCoupon userCoupon = UserCoupon.create(1L, 1L);

        when(userCouponRepository.findById(userCouponId)).thenReturn(Optional.of(userCoupon));
        when(userCouponRepository.save(any(UserCoupon.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // when
        UserCoupon result = userCouponService.useCoupon(userCouponId, orderId);

        // then
        assertThat(result.isUsed()).isTrue();
        verify(userCouponRepository, times(1)).save(any(UserCoupon.class));
    }

    @Test
    @DisplayName("이미 사용한 쿠폰을 다시 사용하면 예외가 발생한다")
    void useCouponAlreadyUsed() {
        // given
        Long userCouponId = 1L;
        Long orderId = 100L;
        UserCoupon userCoupon = UserCoupon.create(1L, 1L);
        userCoupon.use(99L); // 이미 사용됨

        when(userCouponRepository.findById(userCouponId)).thenReturn(Optional.of(userCoupon));

        // when & then
        assertThatThrownBy(() -> userCouponService.useCoupon(userCouponId, orderId))
                .isInstanceOf(CouponException.class);

        verify(userCouponRepository, never()).save(any());
    }

    @Test
    @DisplayName("쿠폰 발급 수를 조회할 수 있다")
    void getCurrentIssueCount() {
        // given
        Long couponId = 1L;
        when(userCouponRepository.countByCouponId(couponId)).thenReturn(75);

        // when
        Integer result = userCouponService.getCurrentIssueCount(couponId);

        // then
        assertThat(result).isEqualTo(75);
        verify(userCouponRepository, times(1)).countByCouponId(couponId);
    }
}
