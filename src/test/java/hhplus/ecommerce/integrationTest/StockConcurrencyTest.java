package hhplus.ecommerce.integrationTest;

import hhplus.ecommerce.product.application.service.ProductService;
import hhplus.ecommerce.product.application.service.StockService;
import hhplus.ecommerce.product.domain.model.Product;
import hhplus.ecommerce.product.domain.model.ProductOption;
import hhplus.ecommerce.product.domain.model.StockReservation;
import hhplus.ecommerce.product.presentation.dto.response.StockResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class StockConcurrencyTest {

    @Autowired
    private ProductService productService;

    @Autowired
    private StockService stockService;

    private Long productOptionId;

    @BeforeEach
    void setUp() {
        Product product = productService.registerProduct(
                "동시성 테스트 상품",
                "전자제품",
                "재고 동시성 테스트",
                "http://image.url",
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
        int threadCount = 50;
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

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

        StockResponse stockResponse = stockService.getStock(productOptionId);

        assertThat(successCount.get() + failCount.get()).isEqualTo(threadCount);
        assertThat(successCount.get() * 2).isLessThanOrEqualTo(100);
        assertThat(stockResponse.getAvailableQuantity()).isGreaterThanOrEqualTo(0);
    }

    @Test
    @DisplayName("동시성 테스트: 재고 예약 확정 처리")
    void concurrentStockConfirmation() throws InterruptedException {
        StockReservation reservation1 = stockService.reserveStock(1L, productOptionId, 10);
        StockReservation reservation2 = stockService.reserveStock(2L, productOptionId, 10);

        ExecutorService executorService = Executors.newFixedThreadPool(2);
        CountDownLatch latch = new CountDownLatch(2);

        executorService.execute(() -> {
            try {
                stockService.confirmStockReservation(reservation1.getStockReservationId());
            } finally {
                latch.countDown();
            }
        });

        executorService.execute(() -> {
            try {
                stockService.confirmStockReservation(reservation2.getStockReservationId());
            } finally {
                latch.countDown();
            }
        });

        latch.await();
        executorService.shutdown();

        StockResponse stockResponse = stockService.getStock(productOptionId);
        assertThat(stockResponse.getPhysicalQuantity()).isEqualTo(80);
    }
}
