package hhplus.ecommerce.integrationTest;

import hhplus.ecommerce.context.IntegrationTestBase;
import hhplus.ecommerce.product.application.service.ProductService;
import hhplus.ecommerce.product.application.service.StockService;
import hhplus.ecommerce.product.domain.model.Product;
import hhplus.ecommerce.product.domain.model.ProductOption;
import hhplus.ecommerce.product.domain.model.StockReservation;
import hhplus.ecommerce.product.domain.repository.ProductOptionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 재고 동시성 테스트
 * TestContainersConfiguration을 사용하여 공유 MySQL 컨테이너에서 테스트
 */
class StockConcurrencyTest extends IntegrationTestBase {

    @Autowired
    private ProductService productService;

    @Autowired
    private StockService stockService;

    @Autowired
    private ProductOptionRepository productOptionRepository;

    private Long productOptionId;

    @BeforeEach
    void setUp() {
        // 재고 100개인 상품 옵션 생성
        Product product = productService.registerProduct(
                "동시성 테스트 상품",
                "전자제품",
                "재고 동시성 테스트용 상품",
                "http://test-stock.url",
                BigDecimal.valueOf(10000),
                true
        );

        ProductOption productOption = productService.createProductOption(
                product.getProductId(),
                "기본 옵션",
                BigDecimal.ZERO,
                100,
                true
        );

        productOptionId = productOption.getProductOptionId();
    }

    @Test
    @DisplayName("동시성 테스트: 여러 스레드가 동시에 재고 예약 시도")
    void concurrentStockReservation() throws InterruptedException {
        // Given: 50개 주문이 각각 2개씩 예약 시도 (총 100개 재고)
        int threadCount = 50;
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        // When: 동시에 재고 예약 시도
        for (int i = 0; i < threadCount; i++) {
            final long orderId = i + 1;
            executorService.execute(() -> {
                try {
                    StockReservation reservation = stockService.reserveStock(orderId, productOptionId, 2);
                    if (reservation != null) {
                        successCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    failCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executorService.shutdown();

        // Then: 모든 예약이 성공 (100개 재고, 50개 주문 * 2개씩 = 100개)
        assertThat(successCount.get()).isEqualTo(50);
        assertThat(failCount.get()).isEqualTo(0);

        // Then: 재고가 0이 됨
        ProductOption updatedOption = productOptionRepository.findById(productOptionId).orElseThrow();
        assertThat(updatedOption.getStockQuantity()).isEqualTo(0);
    }

    @Test
    @DisplayName("동시성 테스트: 재고 부족 시 일부만 성공")
    void concurrentStockReservation_InsufficientStock() throws InterruptedException {
        // Given: 60개 주문이 각각 2개씩 예약 시도 (총 120개 필요, 재고는 100개)
        int threadCount = 60;
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        // When: 동시에 재고 예약 시도
        for (int i = 0; i < threadCount; i++) {
            final long orderId = i + 1;
            executorService.execute(() -> {
                try {
                    StockReservation reservation = stockService.reserveStock(orderId, productOptionId, 2);
                    if (reservation != null) {
                        successCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    failCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executorService.shutdown();

        // Then: 50개만 성공 (100개 재고 / 2개씩 = 50개 주문), 10개는 실패
        assertThat(successCount.get()).isEqualTo(50);
        assertThat(failCount.get()).isEqualTo(10);

        // Then: 재고가 0이 됨
        ProductOption updatedOption = productOptionRepository.findById(productOptionId).orElseThrow();
        assertThat(updatedOption.getStockQuantity()).isEqualTo(0);
    }

    @Test
    @DisplayName("동시성 테스트: 재고 확정 처리")
    void confirmStockReservations() throws InterruptedException {
        // Given: 재고 예약 먼저 실행하고 reservationId 저장
        int reservationCount = 10;
        Long[] reservationIds = new Long[reservationCount];

        for (int i = 0; i < reservationCount; i++) {
            StockReservation reservation = stockService.reserveStock(i + 1L, productOptionId, 2);
            reservationIds[i] = reservation.getStockReservationId();
        }

        // Then: 20개가 예약됨 (10개 주문 * 2개씩)
        ProductOption afterReservation = productOptionRepository.findById(productOptionId).orElseThrow();
        assertThat(afterReservation.getStockQuantity()).isEqualTo(80); // 100 - 20

        // When: 재고 확정 처리
        ExecutorService executorService = Executors.newFixedThreadPool(reservationCount);
        CountDownLatch latch = new CountDownLatch(reservationCount);

        for (int i = 0; i < reservationCount; i++) {
            final Long reservationId = reservationIds[i];
            executorService.execute(() -> {
                try {
                    stockService.confirmStockReservation(reservationId);
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executorService.shutdown();

        // Then: 재고는 그대로 80 (확정은 예약된 재고를 정식으로 차감하는 것)
        ProductOption afterConfirm = productOptionRepository.findById(productOptionId).orElseThrow();
        assertThat(afterConfirm.getStockQuantity()).isEqualTo(80);
    }
}
