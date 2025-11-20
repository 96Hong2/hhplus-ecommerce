package hhplus.ecommerce.unitTest.order.application;

import hhplus.ecommerce.common.presentation.response.PageResponse;
import hhplus.ecommerce.order.application.dto.OrderDetailInfo;
import hhplus.ecommerce.order.application.dto.OrderItemDetailInfo;
import hhplus.ecommerce.order.application.usecase.*;
import hhplus.ecommerce.order.domain.model.Order;
import hhplus.ecommerce.order.domain.model.OrderItem;
import hhplus.ecommerce.order.domain.model.OrderItemStatus;
import hhplus.ecommerce.order.domain.model.OrderStatus;
import hhplus.ecommerce.order.presentation.dto.request.OrderCreateRequest;
import hhplus.ecommerce.order.presentation.dto.request.OrderItemRequest;
import hhplus.ecommerce.order.presentation.dto.request.OrderItemStatusChangeRequest;
import hhplus.ecommerce.order.presentation.dto.request.OrderStatusChangeRequest;
import hhplus.ecommerce.order.presentation.dto.response.OrderCreateResponse;
import hhplus.ecommerce.order.presentation.dto.response.OrderDetailResponse;
import hhplus.ecommerce.order.presentation.dto.response.OrderItemResponse;
import hhplus.ecommerce.order.presentation.dto.response.OrderListResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import hhplus.ecommerce.order.application.service.OrderService;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderService orderService;

    @InjectMocks
    private CreateOrderUseCase createOrderUseCase;

    @InjectMocks
    private GetOrderListUseCase getOrderListUseCase;

    @InjectMocks
    private GetOrderDetailUseCase getOrderDetailUseCase;

    @InjectMocks
    private ChangeOrderStatusUseCase changeOrderStatusUseCase;

    @InjectMocks
    private ChangeOrderItemStatusUseCase changeOrderItemStatusUseCase;

    @Test
    @DisplayName("주문을 생성할 수 있다")
    void createOrder() {
        // given
        Long userId = 1L;
        OrderCreateRequest request = new OrderCreateRequest();
        OrderItemRequest itemRequest = new OrderItemRequest();
        itemRequest.setProductOptionId(100L);
        itemRequest.setQuantity(2);
        request.setItems(List.of(itemRequest));

        var itemInfo = new hhplus.ecommerce.order.application.dto.OrderItemInfo(
                10L, 100L, "상품", "옵션", 2,
                java.math.BigDecimal.valueOf(10000),
                java.math.BigDecimal.valueOf(20000)
        );

        when(orderService.collectOrderItems(any())).thenReturn(List.of(itemInfo));
        when(orderService.calculateTotalAmount(any())).thenReturn(java.math.BigDecimal.valueOf(20000));
        when(orderService.calculateCouponDiscount(isNull(), eq(java.math.BigDecimal.valueOf(20000))))
                .thenReturn(java.math.BigDecimal.valueOf(2000));
        when(orderService.generateOrderNumber(eq(userId))).thenReturn("ORD202501010000001");

        // saveOrder는 전달된 Order를 ID만 채워 반환
        when(orderService.saveOrder(any())).thenAnswer(invocation -> {
            Order o = invocation.getArgument(0);
            return new Order(
                    1L,
                    o.getOrderNumber(),
                    o.getUserId(),
                    o.getTotalAmount(),
                    o.getDiscountAmount(),
                    o.getFinalAmount(),
                    o.getCouponId(),
                    o.getPaymentMethod(),
                    o.getOrderStatus(),
                    o.getCreatedAt(),
                    o.getUpdatedAt(),
                    o.getExpiresAt()
            );
        });

        doNothing().when(orderService).reserveStocks(anyLong(), any());
        when(orderService.saveOrderItems(anyLong(), any())).thenReturn(List.of());

        // when
        OrderCreateResponse result = createOrderUseCase.execute(userId, request);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getOrderId()).isEqualTo(1L);
        assertThat(result.getOrderStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(result.getTotalAmount()).isEqualByComparingTo("20000");
        assertThat(result.getDiscountAmount()).isEqualByComparingTo("2000");
        assertThat(result.getFinalAmount()).isEqualByComparingTo("18000");
        verify(orderService, times(1)).reserveStocks(eq(1L), any());
        verify(orderService, times(1)).saveOrderItems(eq(1L), any());
    }

    @Test
    @DisplayName("주문 목록을 조회할 수 있다")
    void getOrderList() {
        // given
        Long userId = 1L;
        OrderStatus status = OrderStatus.PENDING;
        int page = 0;
        int size = 2;

        var now = LocalDateTime.now();
        var o1 = new Order(
                1L, "ORD1", userId,
                java.math.BigDecimal.valueOf(10000), java.math.BigDecimal.ZERO, java.math.BigDecimal.valueOf(10000),
                null, null, OrderStatus.PENDING, now.minusMinutes(10), now.minusMinutes(10), now.plusMinutes(5)
        );
        var o2 = new Order(
                2L, "ORD2", userId,
                java.math.BigDecimal.valueOf(20000), java.math.BigDecimal.ZERO, java.math.BigDecimal.valueOf(20000),
                null, null, OrderStatus.PENDING, now.minusMinutes(9), now.minusMinutes(9), now.plusMinutes(5)
        );
        var o3 = new Order(
                3L, "ORD3", userId,
                java.math.BigDecimal.valueOf(30000), java.math.BigDecimal.ZERO, java.math.BigDecimal.valueOf(30000),
                null, null, OrderStatus.PENDING, now.minusMinutes(8), now.minusMinutes(8), now.plusMinutes(5)
        );

        when(orderService.getOrdersByUserId(eq(userId), eq(status)))
                .thenReturn(List.of(o1, o2, o3));

        // when
        PageResponse<OrderListResponse> result = getOrderListUseCase.execute(userId, status, page, size);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getTotalElements()).isEqualTo(3);
        assertThat(result.getTotalPages()).isEqualTo(2);
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent().get(0).getOrderId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("주문 상세를 조회할 수 있다")
    void getOrderDetail() {
        // given
        Long orderId = 10L;
        var now = LocalDateTime.now();
        var order = new Order(
                orderId, "ORDX", 99L,
                java.math.BigDecimal.valueOf(10000), java.math.BigDecimal.ZERO, java.math.BigDecimal.valueOf(10000),
                null, null, OrderStatus.PENDING, now.minusMinutes(1), now.minusMinutes(1), now.plusMinutes(10)
        );

        when(orderService.getOrder(eq(orderId))).thenReturn(order);
        when(orderService.getOrderItems(eq(orderId))).thenReturn(List.of());

        var detailInfo = new hhplus.ecommerce.order.application.dto.OrderDetailInfo(
                order.getOrderId(), order.getOrderNumber(), order.getOrderStatus(),
                order.getTotalAmount(), order.getDiscountAmount(), order.getFinalAmount(),
                List.of(), order.getCreatedAt(), order.getExpiresAt()
        );
        when(orderService.toOrderDetailInfo(eq(order), eq(List.of()))).thenReturn(detailInfo);

        // when
        OrderDetailResponse result = getOrderDetailUseCase.execute(orderId);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getOrderId()).isEqualTo(orderId);
        assertThat(result.getOrderStatus()).isEqualTo(OrderStatus.PENDING);
    }

    @Test
    @DisplayName("주문 상태를 변경할 수 있다")
    void changeOrderStatus() {
        // given
        Long userId = 1L;
        Long orderId = 50L;
        OrderStatusChangeRequest request = new OrderStatusChangeRequest(orderId, OrderStatus.PAID);

        var now = LocalDateTime.now();
        var pending = new Order(
                orderId, "ORD-50", userId,
                java.math.BigDecimal.valueOf(10000), java.math.BigDecimal.ZERO, java.math.BigDecimal.valueOf(10000),
                null, null, OrderStatus.PENDING, now.minusMinutes(3), now.minusMinutes(3), now.plusMinutes(10)
        );

        when(orderService.getOrder(eq(orderId))).thenReturn(pending);
        when(orderService.getOrderItems(eq(orderId))).thenReturn(List.of());
        when(orderService.saveOrder(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var updatedInfo = new OrderDetailInfo(
                pending.getOrderId(), pending.getOrderNumber(), OrderStatus.PAID,
                pending.getTotalAmount(), pending.getDiscountAmount(), pending.getFinalAmount(),
                List.of(), pending.getCreatedAt(), pending.getExpiresAt()
        );
        when(orderService.toOrderDetailInfo(any(), eq(List.of()))).thenReturn(updatedInfo);

        // when
        OrderDetailResponse result = changeOrderStatusUseCase.execute(userId, request);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getOrderId()).isEqualTo(orderId);
        assertThat(result.getOrderStatus()).isEqualTo(OrderStatus.PAID);
    }

    @Test
    @DisplayName("주문 아이템 상태를 변경할 수 있다")
    void changeOrderItemStatus() {
        // given
        Long orderItemId = 900L;
        var request = new OrderItemStatusChangeRequest();
        request.setStatus(OrderItemStatus.SHIPPING);

        var item = OrderItem.create(
                77L, 10L, 100L, "상품", "옵션", java.math.BigDecimal.valueOf(5000), 2
        );
        // OrderItem.create는 id가 null이므로 상태 변경 후 저장 시 그대로 사용
        when(orderService.getOrderItem(eq(orderItemId))).thenReturn(item);
        when(orderService.saveOrderItem(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var itemInfo = new OrderItemDetailInfo(
                orderItemId, "상품", "옵션", 2, java.math.BigDecimal.valueOf(5000), OrderItemStatus.SHIPPING
        );
        when(orderService.toOrderItemDetailInfo(any())).thenReturn(itemInfo);

        // when
        OrderItemResponse result = changeOrderItemStatusUseCase.execute(orderItemId, request);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getItemStatus()).isEqualTo(OrderItemStatus.SHIPPING);
        assertThat(result.getProductName()).isEqualTo("상품");
    }
}
