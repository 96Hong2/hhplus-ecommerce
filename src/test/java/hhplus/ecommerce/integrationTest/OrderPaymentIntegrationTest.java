package hhplus.ecommerce.integrationTest;

import hhplus.ecommerce.context.IntegrationTestBase;
import hhplus.ecommerce.order.application.service.PaymentService;
import hhplus.ecommerce.order.application.usecase.CreateOrderUseCase;
import hhplus.ecommerce.order.domain.model.Order;
import hhplus.ecommerce.order.domain.model.OrderStatus;
import hhplus.ecommerce.order.domain.repository.OrderRepository;
import hhplus.ecommerce.order.presentation.dto.request.OrderCreateRequest;
import hhplus.ecommerce.order.presentation.dto.request.OrderItemRequest;
import hhplus.ecommerce.order.presentation.dto.request.PaymentRequest;
import hhplus.ecommerce.order.presentation.dto.response.OrderCreateResponse;
import hhplus.ecommerce.order.presentation.dto.response.PaymentResponse;
import hhplus.ecommerce.point.application.service.PointService;
import hhplus.ecommerce.product.application.service.ProductService;
import hhplus.ecommerce.product.domain.model.Product;
import hhplus.ecommerce.product.domain.model.ProductOption;
import hhplus.ecommerce.product.domain.repository.ProductOptionRepository;
import hhplus.ecommerce.user.domain.model.User;
import hhplus.ecommerce.user.domain.model.UserRole;
import hhplus.ecommerce.user.domain.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * JPA 기반 주문/결제 통합 테스트
 * TestContainersConfiguration을 사용하여 공유 MySQL 컨테이너에서 테스트
 */
class OrderPaymentIntegrationTest extends IntegrationTestBase {

    @Autowired
    private CreateOrderUseCase createOrderUseCase;

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private ProductService productService;

    @Autowired
    private PointService pointService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ProductOptionRepository productOptionRepository;

    private Long userId;
    private Long productOptionId;

    @BeforeEach
    void setUp() {
        // 사용자 생성
        User user = User.create("테스트유저", UserRole.CUSTOMER);
        User savedUser = userRepository.save(user);
        userId = savedUser.getUserId();

        // 포인트 충전
        pointService.chargePoint(userId, BigDecimal.valueOf(100000), "테스트 충전");

        // 상품 및 옵션 생성
        Product product = productService.registerProduct(
                "테스트 상품",
                "전자제품",
                "통합 테스트용 상품",
                "http://test-image.url",
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
    @DisplayName("통합 테스트: 주문 생성 -> 결제 -> 재고 확정 플로우")
    @Transactional
    void orderAndPaymentFlow() {
        // Given: 주문 요청 생성
        OrderItemRequest itemRequest = new OrderItemRequest();
        itemRequest.setProductOptionId(productOptionId);
        itemRequest.setQuantity(2);

        OrderCreateRequest orderRequest = new OrderCreateRequest();
        orderRequest.setItems(List.of(itemRequest));
        orderRequest.setCouponId(null);

        // When: 주문 생성
        OrderCreateResponse orderResponse = createOrderUseCase.execute(userId, orderRequest);

        // Then: 주문이 PENDING 상태로 생성됨
        assertThat(orderResponse).isNotNull();
        assertThat(orderResponse.getOrderStatus()).isEqualTo(OrderStatus.PENDING);

        Long orderId = orderResponse.getOrderId();

        // When: 결제 진행
        PaymentRequest paymentRequest = new PaymentRequest();
        paymentRequest.setPaymentMethod("CREDIT");
        PaymentResponse paymentResponse = paymentService.payOrder(orderId, paymentRequest);

        // Then: 결제 완료 및 주문 상태가 PAID로 변경
        assertThat(paymentResponse.getOrderStatus()).isEqualTo(OrderStatus.PAID);

        // Then: 재고가 차감되었는지 확인
        ProductOption updatedOption = productOptionRepository.findById(productOptionId).orElseThrow();
        assertThat(updatedOption.getStockQuantity()).isEqualTo(98); // 100 - 2
    }

    @Test
    @DisplayName("통합 테스트: 포인트 부족 시 주문 생성은 되나 결제 실패")
    @Transactional
    void paymentFailsWhenInsufficientPoints() {
        // Given: 포인트를 모두 사용
        User user = userRepository.findById(userId).orElseThrow();
        BigDecimal currentBalance = user.getPointBalance();
        pointService.usePoint(userId, currentBalance, null, "포인트 소진");

        // 고가 상품 생성
        Product expensiveProduct = productService.registerProduct(
                "고가 상품",
                "전자제품",
                "비싼 상품",
                "http://expensive.url",
                BigDecimal.valueOf(200000),
                true
        );

        ProductOption expensiveOption = productService.createProductOption(
                expensiveProduct.getProductId(),
                "고가 옵션",
                BigDecimal.ZERO,
                10,
                true
        );

        // When: 주문 생성 (PENDING 상태)
        OrderItemRequest itemRequest = new OrderItemRequest();
        itemRequest.setProductOptionId(expensiveOption.getProductOptionId());
        itemRequest.setQuantity(1);

        OrderCreateRequest orderRequest = new OrderCreateRequest();
        orderRequest.setItems(List.of(itemRequest));
        orderRequest.setCouponId(null);

        OrderCreateResponse orderResponse = createOrderUseCase.execute(userId, orderRequest);

        // Then: 주문은 생성되지만 PENDING 상태
        assertThat(orderResponse.getOrderStatus()).isEqualTo(OrderStatus.PENDING);

        // When & Then: 결제 시도 시 포인트 부족으로 실패
        Long orderId = orderResponse.getOrderId();
        PaymentRequest paymentRequest = new PaymentRequest();
        paymentRequest.setPaymentMethod("POINT");

        try {
            paymentService.payOrder(orderId, paymentRequest);
        } catch (Exception e) {
            // 포인트 부족 예외 발생 확인
            assertThat(e.getMessage()).contains("포인트");
        }

        // Then: 주문 상태는 여전히 PENDING
        Order order = orderRepository.findById(orderId).orElseThrow();
        assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.PENDING);
    }
}
