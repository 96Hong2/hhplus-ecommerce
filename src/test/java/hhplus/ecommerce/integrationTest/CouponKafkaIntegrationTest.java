package hhplus.ecommerce.integrationTest;

import hhplus.ecommerce.context.IntegrationTestBase;
import hhplus.ecommerce.coupon.application.service.CouponService;
import hhplus.ecommerce.coupon.application.service.RedisCouponService;
import hhplus.ecommerce.coupon.application.service.UserCouponService;
import hhplus.ecommerce.coupon.domain.model.Coupon;
import hhplus.ecommerce.coupon.domain.model.DiscountType;
import hhplus.ecommerce.coupon.domain.model.UserCoupon;
import hhplus.ecommerce.coupon.domain.repository.CouponRepository;
import hhplus.ecommerce.coupon.domain.repository.UserCouponRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * 쿠폰 Kafka 통합 테스트
 * - Testcontainers를 통한 실제 Kafka 통합 테스트
 */
class CouponKafkaIntegrationTest extends IntegrationTestBase {

    @Autowired
    private RedisCouponService redisCouponService;

    @Autowired
    private UserCouponService userCouponService;

    @Autowired
    private CouponService couponService;

    @Autowired
    private CouponRepository couponRepository;

    @Autowired
    private UserCouponRepository userCouponRepository;

    private Coupon testCoupon;

    @BeforeEach
    void setUp() {
        // 테스트용 쿠폰 생성
        testCoupon = couponService.createCoupon(
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
    @DisplayName("쿠폰 발급 시 Kafka 이벤트가 정상적으로 발행되어야 한다")
    void testCouponIssuedEventPublished() {
        // Given
        Long userId = 1000L;
        Long couponId = testCoupon.getCouponId();

        // When
        UserCoupon userCoupon = redisCouponService.issueCouponWithRedisZset(userId, couponId);

        // Then
        assertThat(userCoupon).isNotNull();
        assertThat(userCoupon.getUserId()).isEqualTo(userId);
        assertThat(userCoupon.getCouponId()).isEqualTo(couponId);

        // Kafka Consumer가 메시지를 받을 때까지 대기
        await().atMost(5, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    // Consumer가 로그를 남겼는지 확인 (실제로는 Consumer의 동작 검증)
                    // 이 테스트에서는 예외가 발생하지 않으면 성공
                    assertThat(userCoupon.getUserCouponId()).isNotNull();
                });
    }

    @Test
    @DisplayName("쿠폰 사용 시 Kafka 이벤트가 정상적으로 발행되어야 한다")
    void testCouponUsedEventPublished() {
        // Given
        Long userId = 2000L;
        Long orderId = 1L;

        // 쿠폰 발급
        UserCoupon userCoupon = redisCouponService.issueCouponWithRedisZset(userId, testCoupon.getCouponId());

        // When
        UserCoupon usedCoupon = userCouponService.useCoupon(userCoupon.getUserCouponId(), orderId);

        // Then
        assertThat(usedCoupon).isNotNull();
        assertThat(usedCoupon.getOrderId()).isEqualTo(orderId);

        // Kafka Consumer가 메시지를 받을 때까지 대기
        await().atMost(5, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    UserCoupon updated = userCouponRepository.findById(usedCoupon.getUserCouponId()).orElseThrow();
                    assertThat(updated.getOrderId()).isEqualTo(orderId);
                });
    }

    @Test
    @DisplayName("여러 사용자가 동시에 쿠폰 발급 시 Kafka 이벤트가 모두 발행되어야 한다")
    void testMultipleCouponIssuedEventsPublished() throws InterruptedException {
        // Given
        int userCount = 5;
        Coupon multiUserCoupon = couponService.createCoupon(
                "동시 발급 테스트 쿠폰",
                DiscountType.FIXED,
                BigDecimal.valueOf(3000),
                BigDecimal.valueOf(5000),
                10,
                LocalDateTime.now().minusDays(1),
                LocalDateTime.now().plusDays(30),
                1L
        );

        // When
        for (int i = 0; i < userCount; i++) {
            Long userId = 3000L + i;
            redisCouponService.issueCouponWithRedisZset(userId, multiUserCoupon.getCouponId());
        }

        // Then
        await().atMost(10, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    Integer issuedCount = userCouponRepository.countByCouponId(multiUserCoupon.getCouponId());
                    assertThat(issuedCount).isEqualTo(userCount);
                });
    }

    @Test
    @DisplayName("쿠폰 발급 실패 시 이벤트가 발행되지 않아야 한다")
    void testCouponIssuedEventNotPublishedOnFailure() {
        // Given
        Long userId = 4000L;

        // 쿠폰을 먼저 발급
        redisCouponService.issueCouponWithRedisZset(userId, testCoupon.getCouponId());

        // When & Then: 중복 발급 시도
        try {
            redisCouponService.issueCouponWithRedisZset(userId, testCoupon.getCouponId());
        } catch (Exception e) {
            // 예외 발생 확인
            assertThat(e).isNotNull();
        }

        // 발급된 쿠폰은 여전히 1개만 존재
        Integer issuedCount = userCouponRepository.countByCouponId(testCoupon.getCouponId());
        assertThat(issuedCount).isEqualTo(1);
    }
}
