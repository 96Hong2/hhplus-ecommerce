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
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class CouponConcurrencyTest {

    @Autowired
    private CouponService couponService;

    @Autowired
    private UserCouponService userCouponService;

    @Autowired
    private UserRepository userRepository;

    private Long couponId;

    @BeforeEach
    void setUp() {
        Coupon coupon = couponService.createCoupon(
                "선착순 쿠폰",
                DiscountType.FIXED,
                BigDecimal.valueOf(10000),
                BigDecimal.valueOf(50000),
                100,
                LocalDateTime.now(),
                LocalDateTime.now().plusDays(30),
                1L
        );
        couponId = coupon.getCouponId();
    }

    @Test
    @DisplayName("동시성 테스트: 선착순 쿠폰 발급 - 100명만 성공")
    void issueFirstComeCoupon() throws InterruptedException {
        int threadCount = 150;
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            final long userId = i + 1;
            User user = userRepository.save(User.create("testuser", UserRole.CUSTOMER));

            executorService.execute(() -> {
                try {
                    userCouponService.issueFirstComeCoupon(user.getUserId(), couponId);
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

        assertThat(successCount.get()).isEqualTo(100);
        assertThat(failCount.get()).isEqualTo(50);
        assertThat(userCouponService.getCurrentIssueCount(couponId)).isEqualTo(100);
    }

    @Test
    @DisplayName("동시성 테스트: 선착순 쿠폰 발급 - 정확히 한도만큼만 발급")
    void issueFirstComeCouponExactLimit() throws InterruptedException {
        Coupon limitedCoupon = couponService.createCoupon(
                "제한된 선착순 쿠폰",
                DiscountType.PERCENTAGE,
                BigDecimal.valueOf(10),
                BigDecimal.valueOf(30000),
                50,
                LocalDateTime.now(),
                LocalDateTime.now().plusDays(30),
                1L
        );

        int threadCount = 100;
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            final long userId = i + 1000;
            User user = userRepository.save(User.create("testuser", UserRole.CUSTOMER));

            executorService.execute(() -> {
                try {
                    userCouponService.issueFirstComeCoupon(user.getUserId(), limitedCoupon.getCouponId());
                    successCount.incrementAndGet();
                } catch (Exception e) {
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executorService.shutdown();

        assertThat(successCount.get()).isEqualTo(50);
        assertThat(userCouponService.getCurrentIssueCount(limitedCoupon.getCouponId())).isEqualTo(50);
    }

    @Test
    @DisplayName("동시성 테스트: 동일 사용자가 여러 번 시도해도 1개만 발급")
    void issueFirstComeCouponDuplicateUser() throws InterruptedException {
        User user = userRepository.save(User.create("testuser", UserRole.CUSTOMER));

        int threadCount = 10;
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            executorService.execute(() -> {
                try {
                    userCouponService.issueFirstComeCoupon(user.getUserId(), couponId);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executorService.shutdown();

        assertThat(successCount.get()).isEqualTo(1);
    }
}
