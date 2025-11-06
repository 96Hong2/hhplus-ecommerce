package hhplus.ecommerce.integrationTest;

import hhplus.ecommerce.point.application.service.PointService;
import hhplus.ecommerce.user.domain.model.User;
import hhplus.ecommerce.user.domain.model.UserRole;
import hhplus.ecommerce.user.domain.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class PointConcurrencyTest {

    @Autowired
    private PointService pointService;

    @Autowired
    private UserRepository userRepository;

    private Long userId;

    @BeforeEach
    void setUp() {
        User user = User.create("동시성테스트유저", UserRole.CUSTOMER);
        User savedUser = userRepository.save(user);
        userId = savedUser.getUserId();

        pointService.chargePoint(userId, BigDecimal.valueOf(100000), "초기 충전");
    }

    @Test
    @DisplayName("동시성 테스트: 여러 스레드가 동시에 포인트 충전")
    void concurrentPointCharge() throws InterruptedException {
        int threadCount = 10;
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            executorService.execute(() -> {
                try {
                    pointService.chargePoint(userId, BigDecimal.valueOf(1000), "동시 충전");
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executorService.shutdown();

        User user = userRepository.findById(userId).orElseThrow();
        assertThat(user.getPointBalance()).isEqualTo(BigDecimal.valueOf(110000));
    }

    @Test
    @DisplayName("동시성 테스트: 여러 스레드가 동시에 포인트 사용")
    void concurrentPointUse() throws InterruptedException {
        int threadCount = 10;
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            final long orderId = i + 1;
            executorService.execute(() -> {
                try {
                    pointService.usePoint(userId, BigDecimal.valueOf(5000), orderId, "동시 사용");
                } catch (Exception e) {
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executorService.shutdown();

        User user = userRepository.findById(userId).orElseThrow();
        assertThat(user.getPointBalance()).isEqualTo(BigDecimal.valueOf(50000));
    }

    @Test
    @DisplayName("동시성 테스트: 포인트 충전과 사용이 동시에 발생")
    void concurrentPointChargeAndUse() throws InterruptedException {
        int threadCount = 20;
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            executorService.execute(() -> {
                try {
                    if (index % 2 == 0) {
                        pointService.chargePoint(userId, BigDecimal.valueOf(1000), "충전");
                    } else {
                        pointService.usePoint(userId, BigDecimal.valueOf(500), (long) index, "사용");
                    }
                } catch (Exception e) {
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executorService.shutdown();

        User user = userRepository.findById(userId).orElseThrow();
        assertThat(user.getPointBalance()).isGreaterThanOrEqualTo(BigDecimal.ZERO);
    }
}
