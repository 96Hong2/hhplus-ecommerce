package hhplus.ecommerce.unitTest.coupon.repository;

import hhplus.ecommerce.coupon.domain.model.UserCoupon;
import hhplus.ecommerce.coupon.infrastructure.repository.InMemoryUserCouponRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class UserCouponRepositoryTest {

    private InMemoryUserCouponRepository userCouponRepository;

    @BeforeEach
    void setUp() {
        userCouponRepository = new InMemoryUserCouponRepository();
    }

    @Test
    @DisplayName("사용자 쿠폰을 저장할 수 있다")
    void saveUserCoupon() {
        UserCoupon userCoupon = UserCoupon.create(1L, 1L);

        UserCoupon savedUserCoupon = userCouponRepository.save(userCoupon);

        assertThat(savedUserCoupon).isNotNull();
        assertThat(savedUserCoupon.getUserCouponId()).isNotNull();
        assertThat(savedUserCoupon.getUserId()).isEqualTo(1L);
        assertThat(savedUserCoupon.getCouponId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("ID로 사용자 쿠폰을 조회할 수 있다")
    void findById() {
        UserCoupon userCoupon = UserCoupon.create(1L, 1L);
        UserCoupon savedUserCoupon = userCouponRepository.save(userCoupon);

        Optional<UserCoupon> foundUserCoupon = userCouponRepository.findById(savedUserCoupon.getUserCouponId());

        assertThat(foundUserCoupon).isPresent();
        assertThat(foundUserCoupon.get().getUserId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("사용자 ID로 쿠폰 목록을 조회할 수 있다")
    void findByUserId() {
        userCouponRepository.save(UserCoupon.create(1L, 1L));
        userCouponRepository.save(UserCoupon.create(1L, 2L));
        userCouponRepository.save(UserCoupon.create(2L, 1L));

        List<UserCoupon> userCoupons = userCouponRepository.findByUserId(1L);

        assertThat(userCoupons).hasSize(2);
    }

    @Test
    @DisplayName("사용자 ID와 쿠폰 ID로 중복 발급 여부를 확인할 수 있다")
    void findByUserIdAndCouponId() {
        UserCoupon userCoupon = UserCoupon.create(1L, 1L);
        userCouponRepository.save(userCoupon);

        Optional<UserCoupon> found = userCouponRepository.findByUserIdAndCouponId(1L, 1L);
        Optional<UserCoupon> notFound = userCouponRepository.findByUserIdAndCouponId(1L, 2L);

        assertThat(found).isPresent();
        assertThat(notFound).isEmpty();
    }

    @Test
    @DisplayName("쿠폰 ID로 발급 수를 조회할 수 있다")
    void countByCouponId() {
        userCouponRepository.save(UserCoupon.create(1L, 1L));
        userCouponRepository.save(UserCoupon.create(2L, 1L));
        userCouponRepository.save(UserCoupon.create(3L, 1L));

        Integer count = userCouponRepository.countByCouponId(1L);

        assertThat(count).isEqualTo(3);
    }

    @Test
    @DisplayName("사용 여부로 사용자 쿠폰 목록을 필터링할 수 있다")
    void findByUserIdAndIsUsed() {
        UserCoupon userCoupon1 = UserCoupon.create(1L, 1L);
        UserCoupon userCoupon2 = UserCoupon.create(1L, 2L);
        userCoupon2.use(100L);

        userCouponRepository.save(userCoupon1);
        userCouponRepository.save(userCoupon2);

        List<UserCoupon> unusedCoupons = userCouponRepository.findByUserIdAndIsUsed(1L, false);
        List<UserCoupon> usedCoupons = userCouponRepository.findByUserIdAndIsUsed(1L, true);

        assertThat(unusedCoupons).hasSize(1);
        assertThat(usedCoupons).hasSize(1);
    }
}
