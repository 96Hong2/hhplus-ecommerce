package hhplus.ecommerce.integrationTest;

import hhplus.ecommerce.context.IntegrationTestBase;
import hhplus.ecommerce.order.domain.model.Order;
import hhplus.ecommerce.order.domain.repository.OrderRepository;
import hhplus.ecommerce.product.application.service.StockService;
import hhplus.ecommerce.product.domain.model.Product;
import hhplus.ecommerce.product.domain.model.ProductOption;
import hhplus.ecommerce.product.domain.model.StockReservation;
import hhplus.ecommerce.product.domain.repository.ProductOptionRepository;
import hhplus.ecommerce.product.domain.repository.ProductRepository;
import hhplus.ecommerce.product.domain.repository.StockReservationRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Redisson 분산 락 기반 재고 예약 통합 테스트
 * IntegrationTestBase를 상속하여 Testcontainer 설정 사용
 */
class StockReservationWithLockTest extends IntegrationTestBase {

    @Autowired
    private StockService stockService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ProductOptionRepository productOptionRepository;

    @Autowired
    private StockReservationRepository stockReservationRepository;

    @Autowired
    private OrderRepository orderRepository;

    private Long productId;
    private Long productOptionId;
    private List<Long> orderIds;

    @BeforeEach
    void setUp() {
        // 테스트용 상품 생성
        Product product = productRepository.save(
                Product.create("테스트 상품", "전자기기", "동시성 테스트용 상품", null, BigDecimal.valueOf(100000), true)
        );
        productId = product.getProductId();

        // 재고 100개인 상품 옵션 생성
        ProductOption productOption = productOptionRepository.save(
                ProductOption.create(productId, "기본 옵션", BigDecimal.ZERO, 100, true)
        );
        productOptionId = productOption.getProductOptionId();

        // 테스트용 주문 150개 생성
        orderIds = new ArrayList<>();
        for (int i = 0; i < 150; i++) {
            Order order = orderRepository.save(
                    Order.create("ORD-TEST-" + System.currentTimeMillis() + "-" + i, 1L, BigDecimal.valueOf(100000), BigDecimal.ZERO, null)
            );
            orderIds.add(order.getOrderId());
        }
    }

    @AfterEach
    void tearDown() {
        // 외래키 순서 고려하여 데이터 정리
        stockReservationRepository.deleteAll();
        orderRepository.deleteAll();
        productOptionRepository.deleteAll();
        productRepository.deleteAll();

        // 초기화
        productId = null;
        productOptionId = null;
        orderIds = null;
    }

    @Test
    @DisplayName("정상적인 재고 예약 - 분산 락 사용")
    void reserveStockWithRLock_Success() {
        // Given: 주문 ID와 상품 옵션 ID, 예약 수량
        Long orderId = orderIds.get(0);
        int quantity = 5;

        // When: 재고 예약
        StockReservation reservation = stockService.reserveStockWithRLock(orderId, productOptionId, quantity);

        // Then: 예약 성공 검증
        assertThat(reservation).isNotNull();
        assertThat(reservation.getOrderId()).isEqualTo(orderId);
        assertThat(reservation.getProductOptionId()).isEqualTo(productOptionId);
        assertThat(reservation.getReservedQuantity()).isEqualTo(quantity);

        // 물리 재고가 95로 감소했는지 확인
        ProductOption productOption = productOptionRepository.findById(productOptionId).orElseThrow();
        assertThat(productOption.getStockQuantity()).isEqualTo(95);
    }

    @Test
    @DisplayName("재고 부족 시 예외 발생 - 분산 락 사용")
    void reserveStockWithRLock_InsufficientStock() {
        // Given: 재고(100개)보다 많은 수량(150개) 요청
        Long orderId = orderIds.get(0);
        int quantity = 150;

        // When & Then: 재고 부족 예외 발생
        assertThatThrownBy(() -> stockService.reserveStockWithRLock(orderId, productOptionId, quantity))
                .hasMessageContaining("재고가 부족합니다");

        // 재고가 변경되지 않았는지 확인
        ProductOption productOption = productOptionRepository.findById(productOptionId).orElseThrow();
        assertThat(productOption.getStockQuantity()).isEqualTo(100);
    }

    @Test
    @DisplayName("동시성 테스트: 100개 재고에 150개 주문 - 100개만 성공")
    void reserveStockWithRLock_Concurrency() throws InterruptedException {
        // Given: 150개의 주문이 동시에 각각 1개씩 재고 예약 시도
        int threadCount = 150;
        ExecutorService executorService = Executors.newFixedThreadPool(50);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        // When: 150개 주문이 동시에 재고 예약 시도
        for (int i = 0; i < threadCount; i++) {
            final Long orderId = orderIds.get(i);
            executorService.execute(() -> {
                try {
                    stockService.reserveStockWithRLock(orderId, productOptionId, 1);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        // 모든 스레드 완료 대기
        latch.await();
        executorService.shutdown();

        // Then: 정확히 100개만 성공, 50개는 실패
        assertThat(successCount.get()).isEqualTo(100);
        assertThat(failCount.get()).isEqualTo(50);

        // DB에 100개의 예약이 저장되었는지 확인
        long reservationCount = stockReservationRepository.count();
        assertThat(reservationCount).isEqualTo(100);

        // 물리 재고가 0으로 감소했는지 확인
        ProductOption productOption = productOptionRepository.findById(productOptionId).orElseThrow();
        assertThat(productOption.getStockQuantity()).isEqualTo(0);
    }
}
