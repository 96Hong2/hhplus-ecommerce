package hhplus.ecommerce.unitTest.coupon.repository;

import hhplus.ecommerce.coupon.domain.model.Coupon;
import hhplus.ecommerce.coupon.domain.model.DiscountType;
import hhplus.ecommerce.coupon.domain.model.UserCoupon;
import hhplus.ecommerce.coupon.domain.model.UserCouponStatus;
import hhplus.ecommerce.coupon.domain.repository.CouponRepository;
import hhplus.ecommerce.coupon.domain.repository.UserCouponRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class CouponJpaRepositoriesTest {

    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0");

    @DynamicPropertySource
    static void overrideProps(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
    }

    @Autowired
    private CouponRepository couponJpaRepository;

    @Autowired
    private UserCouponRepository userCouponJpaRepository;

    private Coupon createValidCoupon(int maxIssue) {
        return Coupon.create(
                "쿠폰",
                DiscountType.FIXED,
                BigDecimal.valueOf(1000),
                BigDecimal.valueOf(0),
                maxIssue,
                LocalDateTime.now().minusDays(1),
                LocalDateTime.now().plusDays(1),
                1L
        );
    }

    @Test
    @DisplayName("JPA: 쿠폰 생성/잠금조회/유효/발급가능 조회")
    void couponQueries() {
        Coupon c1 = couponJpaRepository.save(createValidCoupon(10));
        Coupon c2 = couponJpaRepository.save(createValidCoupon(1));

        // 비관적 락 조회
        Optional<Coupon> locked = couponJpaRepository.findByIdWithLock(c1.getCouponId());
        assertThat(locked).isPresent();

        // 현재 유효 쿠폰
        List<Coupon> available = couponJpaRepository.findAvailableCoupons(LocalDateTime.now());
        assertThat(available).isNotEmpty();

        // 발급가능 쿠폰(issuedCount < maxIssueCount)
        List<Coupon> issuable = couponJpaRepository.findIssuableCoupons();
        assertThat(issuable).hasSize(2);
    }

    @Test
    @DisplayName("JPA: 유저쿠폰 저장 및 조회")
    void userCouponQueries() {
        Coupon coupon = couponJpaRepository.save(createValidCoupon(5));

        UserCoupon uc1 = userCouponJpaRepository.save(UserCoupon.create(10L, coupon.getCouponId()));
        UserCoupon uc2 = userCouponJpaRepository.save(UserCoupon.create(11L, coupon.getCouponId()));

        List<UserCoupon> byUser = userCouponJpaRepository.findByUserId(10L);
        assertThat(byUser).hasSize(1);

        Optional<UserCoupon> byUserAndCoupon = userCouponJpaRepository.findByUserIdAndCouponId(10L, coupon.getCouponId());
        assertThat(byUserAndCoupon).isPresent();

        List<UserCoupon> available = userCouponJpaRepository.findAvailableByUserId(10L);
        assertThat(available).hasSize(1);

        List<UserCoupon> byCoupon = userCouponJpaRepository.findByCouponId(coupon.getCouponId());
        assertThat(byCoupon).hasSize(2);

        // 상태로 조회
        List<UserCoupon> actives = userCouponJpaRepository.findByUserIdAndStatus(10L, UserCouponStatus.ACTIVE);
        assertThat(actives).hasSize(1);
    }
}

