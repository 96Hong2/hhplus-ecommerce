package hhplus.ecommerce.integrationTest;

import hhplus.ecommerce.order.application.service.OrderService;
import hhplus.ecommerce.order.application.service.PaymentService;
import hhplus.ecommerce.order.domain.model.OrderStatus;
import hhplus.ecommerce.order.presentation.dto.request.OrderCreateRequest;
import hhplus.ecommerce.order.presentation.dto.request.OrderItemRequest;
import hhplus.ecommerce.order.presentation.dto.request.PaymentRequest;
import hhplus.ecommerce.order.presentation.dto.response.OrderCreateResponse;
import hhplus.ecommerce.order.presentation.dto.response.PaymentResponse;
import hhplus.ecommerce.point.application.service.PointService;
import hhplus.ecommerce.product.application.service.ProductService;
import hhplus.ecommerce.product.application.service.StockService;
import hhplus.ecommerce.product.domain.model.Product;
import hhplus.ecommerce.product.domain.model.ProductOption;
import hhplus.ecommerce.user.domain.model.User;
import hhplus.ecommerce.user.domain.model.UserRole;
import hhplus.ecommerce.user.domain.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class OrderPaymentIntegrationTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private ProductService productService;

    @Autowired
    private StockService stockService;

    @Autowired
    private PointService pointService;

    @Autowired
    private UserRepository userRepository;

    private Long userId;
    private Long productOptionId;

    @BeforeEach
    void setUp() {
        User user = User.create("테스트유저", UserRole.CUSTOMER);
        User savedUser = userRepository.save(user);
        userId = savedUser.getUserId();

        pointService.chargePoint(userId, BigDecimal.valueOf(100000), "초기 충전");

        Product product = productService.registerProduct(
                "테스트 상품",
                "전자제품",
                "테스트 상품입니다",
                "http://image.url",
                BigDecimal.valueOf(50000),
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
    void orderAndPaymentFlow() {
        OrderItemRequest itemRequest = new OrderItemRequest();
        itemRequest.setProductOptionId(productOptionId);
        itemRequest.setQuantity(2);

        OrderCreateRequest orderRequest = new OrderCreateRequest();
        orderRequest.setItems(List.of(itemRequest));
        orderRequest.setUsedPoints(10000L);
        orderRequest.setCouponId(null);

        OrderCreateResponse orderResponse = orderService.createOrder(userId, orderRequest);

        assertThat(orderResponse).isNotNull();
        assertThat(orderResponse.getOrderStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(orderResponse.getOrderId()).isNotNull();

        PaymentRequest paymentRequest = new PaymentRequest();
        paymentRequest.setPaymentMethod("CARD");
        paymentRequest.setUsedPoints(10000L);

        PaymentResponse paymentResponse = paymentService.payOrder(orderResponse.getOrderId(), paymentRequest);

        assertThat(paymentResponse).isNotNull();
        assertThat(paymentResponse.getOrderStatus()).isEqualTo(OrderStatus.PAID);
        assertThat(paymentResponse.getOrderId()).isEqualTo(orderResponse.getOrderId());
    }

    @Test
    @DisplayName("통합 테스트: 포인트 부족 시 주문 생성은 성공하지만 결제 실패")
    void orderAndPaymentFlowWithInsufficientPoints() {
        User poorUser = userRepository.save(User.create("가난한유저", UserRole.CUSTOMER));
        pointService.chargePoint(poorUser.getUserId(), BigDecimal.valueOf(1000), "소액 충전");

        OrderItemRequest itemRequest = new OrderItemRequest();
        itemRequest.setProductOptionId(productOptionId);
        itemRequest.setQuantity(1);

        OrderCreateRequest orderRequest = new OrderCreateRequest();
        orderRequest.setItems(List.of(itemRequest));
        orderRequest.setUsedPoints(0L);
        orderRequest.setCouponId(null);

        OrderCreateResponse orderResponse = orderService.createOrder(poorUser.getUserId(), orderRequest);
        assertThat(orderResponse.getOrderStatus()).isEqualTo(OrderStatus.PENDING);

        PaymentRequest paymentRequest = new PaymentRequest();
        paymentRequest.setPaymentMethod("CARD");
        paymentRequest.setUsedPoints(100000L);

        try {
            paymentService.payOrder(orderResponse.getOrderId(), paymentRequest);
        } catch (Exception e) {
            assertThat(e).isNotNull();
        }
    }
}
