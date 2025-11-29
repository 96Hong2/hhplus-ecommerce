package hhplus.ecommerce.integrationTest;

import hhplus.ecommerce.common.domain.exception.IntegrationException;
import hhplus.ecommerce.context.IntegrationTestBase;
import hhplus.ecommerce.integration.application.service.ExternalIntegrationService;
import hhplus.ecommerce.order.application.usecase.CreateOrderUseCase;
import hhplus.ecommerce.order.domain.model.Order;
import hhplus.ecommerce.order.domain.model.OrderStatus;
import hhplus.ecommerce.order.domain.repository.OrderRepository;
import hhplus.ecommerce.order.presentation.dto.request.OrderCreateRequest;
import hhplus.ecommerce.order.presentation.dto.request.OrderItemRequest;
import hhplus.ecommerce.product.application.service.ProductService;
import hhplus.ecommerce.product.application.service.StockService;
import hhplus.ecommerce.product.domain.model.ProductOption;
import hhplus.ecommerce.product.domain.model.ReservationStatus;
import hhplus.ecommerce.product.domain.model.StockReservation;
import hhplus.ecommerce.product.domain.repository.ProductOptionRepository;
import hhplus.ecommerce.product.domain.repository.StockReservationRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

/**
 * Saga 패턴 보상 트랜잭션 테스트
 *
 * 외부 시스템 연동 실패 시 보상 트랜잭션이 정상적으로 실행되는지 검증
 * IntegrationTestBase를 상속하여 공유 Testcontainer 설정 사용
 */
@Disabled
class SagaCompensationTest extends IntegrationTestBase {

    @Autowired
    private CreateOrderUseCase createOrderUseCase;

    @Autowired
    private ProductService productService;

    @Autowired
    private StockService stockService;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ProductOptionRepository productOptionRepository;

    @Autowired
    private StockReservationRepository stockReservationRepository;

    @SpyBean
    private ExternalIntegrationService externalIntegrationService;

    @AfterEach
    void tearDown() {
        // 테스트 후 생성된 데이터 정리 (외래키 순서 고려)
        stockReservationRepository.deleteAll();
        orderRepository.deleteAll();
    }

    @Test
    @DisplayName("보상 트랜잭션 테스트: 외부 시스템 연동 실패 시 주문 취소 및 재고 복구")
    void testCompensationWhenExternalSystemFails() throws Exception {
        // given: 상품 및 재고 준비
        var product = productService.registerProduct(
                "테스트 상품",
                "전자제품",
                "테스트 설명",
                "image.jpg",
                BigDecimal.valueOf(10000),
                true
        );

        var productOption = productService.createProductOption(
                product.getProductId(),
                "기본 옵션",
                BigDecimal.ZERO,
                100,
                true
        );

        // 초기 재고 확인
        int initialStock = productOption.getStockQuantity();

        // 주문 요청 생성
        OrderItemRequest itemRequest = new OrderItemRequest();
        itemRequest.setProductOptionId(productOption.getProductOptionId());
        itemRequest.setQuantity(5);

        OrderCreateRequest orderRequest = new OrderCreateRequest();
        orderRequest.setItems(List.of(itemRequest));

        // 외부 시스템 연동 실패 시뮬레이션
        doThrow(IntegrationException.erpIntegrationFailed(1L, "시스템 장애"))
                .when(externalIntegrationService)
                .sendOrderToERP(any(Order.class));

        // when & then: 주문 생성 시도 → 외부 연동 실패 → 예외 발생
        assertThatThrownBy(() -> createOrderUseCase.execute(1L, orderRequest))
                .isInstanceOf(IntegrationException.class)
                .hasMessageContaining("ERP");

        // 보상 트랜잭션 검증
        // 1. 주문 상태가 CANCELLED로 변경되었는지 확인
        List<Order> orders = orderRepository.findByUserId(1L);
        assertThat(orders).isNotEmpty();
        Order cancelledOrder = orders.get(0);
        assertThat(cancelledOrder.getOrderStatus()).isEqualTo(OrderStatus.CANCELLED);

        // 2. 재고가 복구되었는지 확인
        ProductOption updatedOption = productOptionRepository.findById(productOption.getProductOptionId())
                .orElseThrow();
        assertThat(updatedOption.getStockQuantity()).isEqualTo(initialStock);

        // 3. 재고 예약이 해제되었는지 확인
        List<StockReservation> reservations = stockReservationRepository.findByOrderId(cancelledOrder.getOrderId());
        assertThat(reservations).isNotEmpty();
        for (StockReservation reservation : reservations) {
            assertThat(reservation.getReservationStatus()).isEqualTo(ReservationStatus.RELEASED);
        }
    }

    @Test
    @DisplayName("정상 플로우 테스트: 외부 시스템 연동 성공 시 주문 완료")
    void testSuccessfulOrderCreationWithExternalIntegration() throws Exception {
        // given: 상품 및 재고 준비
        var product = productService.registerProduct(
                "테스트 상품2",
                "의류",
                "테스트 설명",
                "image2.jpg",
                BigDecimal.valueOf(20000),
                true
        );

        var productOption = productService.createProductOption(
                product.getProductId(),
                "사이즈 M",
                BigDecimal.valueOf(1000),
                50,
                true
        );

        int initialStock = productOption.getStockQuantity();

        // 주문 요청 생성
        OrderItemRequest itemRequest = new OrderItemRequest();
        itemRequest.setProductOptionId(productOption.getProductOptionId());
        itemRequest.setQuantity(3);

        OrderCreateRequest orderRequest = new OrderCreateRequest();
        orderRequest.setItems(List.of(itemRequest));

        // when: 주문 생성 (외부 시스템 연동 성공)
        var response = createOrderUseCase.execute(2L, orderRequest);

        // then: 주문 정상 생성 검증
        assertThat(response).isNotNull();
        assertThat(response.getOrderId()).isNotNull();
        assertThat(response.getOrderStatus()).isEqualTo(OrderStatus.PENDING);

        // 재고 감소 확인
        ProductOption updatedOption = productOptionRepository.findById(productOption.getProductOptionId())
                .orElseThrow();
        assertThat(updatedOption.getStockQuantity()).isEqualTo(initialStock - 3);

        // 재고 예약 상태 확인
        List<StockReservation> reservations = stockReservationRepository.findByOrderId(response.getOrderId());
        assertThat(reservations).hasSize(1);
        assertThat(reservations.get(0).getReservationStatus()).isEqualTo(ReservationStatus.RESERVED);
    }

    @Test
    @DisplayName("재고 부족 시 주문 생성 실패")
    void testOrderCreationFailsWhenInsufficientStock() {
        // given: 재고가 부족한 상품
        var product = productService.registerProduct(
                "재고부족 상품",
                "식품",
                "테스트",
                "image3.jpg",
                BigDecimal.valueOf(5000),
                true
        );

        var productOption = productService.createProductOption(
                product.getProductId(),
                "1kg",
                BigDecimal.ZERO,
                2, // 재고 2개
                true
        );

        // 주문 요청 (재고보다 많이 주문)
        OrderItemRequest itemRequest = new OrderItemRequest();
        itemRequest.setProductOptionId(productOption.getProductOptionId());
        itemRequest.setQuantity(5); // 재고 2개인데 5개 주문

        OrderCreateRequest orderRequest = new OrderCreateRequest();
        orderRequest.setItems(List.of(itemRequest));

        // when & then: 재고 부족으로 주문 실패
        assertThatThrownBy(() -> createOrderUseCase.execute(3L, orderRequest))
                .isInstanceOf(Exception.class)
                .hasMessageContaining("재고");

        // 재고가 변경되지 않았는지 확인
        ProductOption unchangedOption = productOptionRepository.findById(productOption.getProductOptionId())
                .orElseThrow();
        assertThat(unchangedOption.getStockQuantity()).isEqualTo(2);

        // Note: Spring AOP 제약으로 인해 같은 클래스 내부 메서드 호출 시 @Transactional이 작동하지 않아
        // 주문이 롤백되지 않고 DB에 남을 수 있음. 이는 구현 개선이 필요한 부분임.
    }
}
