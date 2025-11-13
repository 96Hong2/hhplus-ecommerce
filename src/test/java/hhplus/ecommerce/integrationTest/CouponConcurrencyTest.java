package hhplus.ecommerce.integrationTest;

import hhplus.ecommerce.coupon.application.service.CouponService;
import hhplus.ecommerce.coupon.application.service.UserCouponService;
import hhplus.ecommerce.coupon.domain.model.Coupon;
import hhplus.ecommerce.coupon.domain.model.DiscountType;
import hhplus.ecommerce.user.domain.model.User;
import hhplus.ecommerce.user.domain.model.UserRole;
import hhplus.ecommerce.user.domain.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * JPA 기반 쿠폰 동시성 테스트
 * Testcontainers를 사용하여 실제 MySQL 환경에서 동시성 제어 검증
 */
@SpringBootTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class CouponConcurrencyTest {

    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0");

    @DynamicPropertySource
    static void overrideProps(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
    }

    @Autowired
    private CouponService couponService;

    @Autowired
    private UserCouponService userCouponService;

    @Autowired
    private UserRepository userRepository;

    private Long couponId;
    private List<Long> userIds;

    @BeforeEach
    void setUp() {
        // 관리자 유저 생성 (쿠폰 생성용)
        User admin = userRepository.save(User.create("admin", UserRole.ADMIN));

        // 선착순 쿠폰 생성 (100개 한정)
        Coupon coupon = couponService.createCoupon(
                "선착순 쿠폰",
                DiscountType.FIXED,
                BigDecimal.valueOf(10000),
                BigDecimal.valueOf(50000),
                100,
                LocalDateTime.now(),
                LocalDateTime.now().plusDays(30),
                admin.getUserId()
        );
        couponId = coupon.getCouponId();

        // 테스트용 유저 150명 생성
        userIds = new ArrayList<>();
        for (int i = 0; i < 150; i++) {
            User user = userRepository.save(User.create("user" + i, UserRole.CUSTOMER));
            userIds.add(user.getUserId());
        }
    }

    @Test
    @DisplayName("동시성 테스트: 선착순 쿠폰 발급 - 100명만 성공")
    void issueFirstComeCoupon_OnlyHundredSuccess() throws InterruptedException {
        int threadCount = 150;
        ExecutorService executorService = Executors.newFixedThreadPool(50);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        // 150명이 동시에 쿠폰 발급 시도
        for (int i = 0; i < threadCount; i++) {
            final Long userId = userIds.get(i);
            executorService.execute(() -> {
                try {
                    userCouponService.issueFirstComeCoupon(userId, couponId);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executorService.shutdown();

        // Then: 정확히 100명만 성공, 50명은 실패
        assertThat(successCount.get()).isEqualTo(100);
        assertThat(failCount.get()).isEqualTo(50);
        assertThat(userCouponService.getCurrentIssueCount(couponId)).isEqualTo(100);
    }

    @Test
    @DisplayName("동시성 테스트: 동일 사용자가 여러 번 시도해도 1개만 발급")
    void issueFirstComeCoupon_SameUserOnlyOnce() throws InterruptedException {
        Long singleUserId = userIds.get(0);

        int threadCount = 10;
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);

        // 동일 사용자가 10번 동시 시도
        for (int i = 0; i < threadCount; i++) {
            executorService.execute(() -> {
                try {
                    userCouponService.issueFirstComeCoupon(singleUserId, couponId);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    // 중복 발급 시도는 예외 발생
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executorService.shutdown();

        // Then: 1번만 성공
        assertThat(successCount.get()).isEqualTo(1);
    }

    @Test
    @DisplayName("동시성 테스트: 선착순 쿠폰 발급 - 정확히 한도만큼만 발급")
    void issueFirstComeCoupon_ExactLimit() throws InterruptedException {
        // Given: 50개 한정 쿠폰 생성
        User admin = userRepository.save(User.create("admin2", UserRole.ADMIN));
        Coupon limitedCoupon = couponService.createCoupon(
                "제한된 선착순 쿠폰",
                DiscountType.PERCENTAGE,
                BigDecimal.valueOf(10),
                BigDecimal.valueOf(30000),
                50,
                LocalDateTime.now(),
                LocalDateTime.now().plusDays(30),
                admin.getUserId()
        );

        int threadCount = 100;
        ExecutorService executorService = Executors.newFixedThreadPool(50);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);

        // When: 100명이 동시에 쿠폰 발급 시도
        for (int i = 0; i < threadCount; i++) {
            final Long userId = userIds.get(i);
            executorService.execute(() -> {
                try {
                    userCouponService.issueFirstComeCoupon(userId, limitedCoupon.getCouponId());
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    // 발급 실패
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executorService.shutdown();

        // Then: 정확히 50명만 성공
        assertThat(successCount.get()).isEqualTo(50);
        assertThat(userCouponService.getCurrentIssueCount(limitedCoupon.getCouponId())).isEqualTo(50);
    }
}
