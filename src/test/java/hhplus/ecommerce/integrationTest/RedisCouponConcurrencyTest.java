package hhplus.ecommerce.integrationTest;

import hhplus.ecommerce.context.IntegrationTestBase;
import hhplus.ecommerce.coupon.application.service.CouponService;
import hhplus.ecommerce.coupon.application.service.RedisCouponService;
import hhplus.ecommerce.coupon.domain.model.Coupon;
import hhplus.ecommerce.coupon.domain.model.DiscountType;
import hhplus.ecommerce.coupon.domain.repository.CouponRepository;
import hhplus.ecommerce.coupon.domain.repository.UserCouponRepository;
import hhplus.ecommerce.user.domain.model.User;
import hhplus.ecommerce.user.domain.model.UserRole;
import hhplus.ecommerce.user.domain.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * Redis SET 기반 쿠폰 동시성 테스트
 * IntegrationTestBase를 상속하여 공유 Testcontainer 설정 사용
 */
class RedisCouponConcurrencyTest extends IntegrationTestBase {

    @Autowired
    private CouponService couponService;

    @Autowired
    private RedisCouponService redisCouponService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CouponRepository couponRepository;

    @Autowired
    private UserCouponRepository userCouponRepository;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    private Long adminId;
    private Long couponId;
    private List<Long> userIds;

    @BeforeEach
    void setUp() {
        // 테스트용 관리자 생성
        User admin = userRepository.save(User.create("쿠폰관리자_Redis", UserRole.ADMIN));
        adminId = admin.getUserId();

        // 선착순 쿠폰 생성 (100개 한정)
        Coupon coupon = couponService.createCoupon(
                "Redis 선착순 쿠폰",
                DiscountType.FIXED,
                BigDecimal.valueOf(10000),
                BigDecimal.valueOf(50000),
                100,
                LocalDateTime.now(),
                LocalDateTime.now().plusDays(30),
                admin.getUserId()
        );
        couponId = coupon.getCouponId();

        // Redis 데이터 초기화
        redisCouponService.clearCouponIssueData(couponId);

        // 테스트용 유저 150명 생성
        userIds = new ArrayList<>();
        for (int i = 0; i < 150; i++) {
            User user = userRepository.save(User.create("Redis쿠폰유저" + i, UserRole.CUSTOMER));
            userIds.add(user.getUserId());
        }
    }

    @AfterEach
    void tearDown() {
        // Redis 데이터 정리
        if (couponId != null) {
            redisCouponService.clearCouponIssueData(couponId);
        }

        // 테스트 후 생성된 데이터 정리 (외래키 순서 고려)
        userCouponRepository.deleteAll(); // 사용자 쿠폰 먼저 삭제
        if (couponId != null) {
            couponRepository.deleteById(couponId); // 쿠폰 삭제
        }
        if (userIds != null && !userIds.isEmpty()) {
            userRepository.deleteAllById(userIds); // 사용자 삭제
        }
        if (adminId != null) {
            userRepository.deleteById(adminId); // 관리자 삭제
        }
    }

    @Test
    @DisplayName("Redis 동시성 테스트: 선착순 쿠폰 발급 - 100명만 성공")
    void issueFirstComeCouponWithRedis_OnlyHundredSuccess() throws InterruptedException {
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
                    redisCouponService.issueCouponWithRedis(userId, couponId);
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

        // 비동기 DB 저장 완료 대기 (최대 30초)
        await().atMost(30, TimeUnit.SECONDS)
            .pollInterval(200, TimeUnit.MILLISECONDS)
            .untilAsserted(() -> {
                assertThat(userCouponRepository.countByCouponId(couponId))
                    .isEqualTo(100);
            });

        // Then: 정확히 100명만 성공, 50명은 실패
        assertThat(successCount.get()).isEqualTo(100);
        assertThat(failCount.get()).isEqualTo(50);

        // Redis와 DB 모두 100개 확인
        assertThat(redisCouponService.getIssuedCount(couponId)).isEqualTo(100L);
        assertThat(userCouponRepository.countByCouponId(couponId)).isEqualTo(100);
    }

    @Test
    @DisplayName("Redis 동시성 테스트: 동일 사용자가 여러 번 시도해도 1개만 발급")
    void issueFirstComeCouponWithRedis_SameUserOnlyOnce() throws InterruptedException {
        Long singleUserId = userIds.get(0);

        int threadCount = 10;
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);

        // 동일 사용자가 10번 동시 시도
        for (int i = 0; i < threadCount; i++) {
            executorService.execute(() -> {
                try {
                    redisCouponService.issueCouponWithRedis(singleUserId, couponId);
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

        // 비동기 DB 저장 완료 대기 (최대 30초)
        await().atMost(30, TimeUnit.SECONDS)
            .pollInterval(200, TimeUnit.MILLISECONDS)
            .untilAsserted(() -> {
                assertThat(userCouponRepository.countByCouponId(couponId))
                    .isEqualTo(1);
            });

        // Then: 1번만 성공
        assertThat(successCount.get()).isEqualTo(1);
        assertThat(redisCouponService.isAlreadyIssued(singleUserId, couponId)).isTrue();
        assertThat(userCouponRepository.countByCouponId(couponId)).isEqualTo(1);
    }

    @Test
    @DisplayName("Redis 동시성 테스트: 선착순 쿠폰 발급 - 정확히 한도만큼만 발급")
    void issueFirstComeCouponWithRedis_ExactLimit() throws InterruptedException {
        // Given: 50개 한정 쿠폰 생성
        User admin = userRepository.save(User.create("admin2_redis", UserRole.ADMIN));
        Coupon limitedCoupon = couponService.createCoupon(
                "Redis 제한된 선착순 쿠폰",
                DiscountType.PERCENTAGE,
                BigDecimal.valueOf(10),
                BigDecimal.valueOf(30000),
                50,
                LocalDateTime.now(),
                LocalDateTime.now().plusDays(30),
                admin.getUserId()
        );
        Long limitedCouponId = limitedCoupon.getCouponId();
        redisCouponService.clearCouponIssueData(limitedCouponId);

        int threadCount = 100;
        ExecutorService executorService = Executors.newFixedThreadPool(50);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);

        // When: 100명이 동시에 쿠폰 발급 시도
        for (int i = 0; i < threadCount; i++) {
            final Long userId = userIds.get(i);
            executorService.execute(() -> {
                try {
                    redisCouponService.issueCouponWithRedis(userId, limitedCouponId);
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

        // 비동기 DB 저장 완료 대기 (최대 30초)
        await().atMost(30, TimeUnit.SECONDS)
            .pollInterval(200, TimeUnit.MILLISECONDS)
            .untilAsserted(() -> {
                assertThat(userCouponRepository.countByCouponId(limitedCouponId))
                    .isEqualTo(50);
            });

        // Then: 정확히 50명만 성공
        assertThat(successCount.get()).isEqualTo(50);
        assertThat(redisCouponService.getIssuedCount(limitedCouponId)).isEqualTo(50L);
        assertThat(userCouponRepository.countByCouponId(limitedCouponId)).isEqualTo(50);

        // Cleanup
        redisCouponService.clearCouponIssueData(limitedCouponId);
        couponRepository.deleteById(limitedCouponId);
        userRepository.deleteById(admin.getUserId());
    }
}
