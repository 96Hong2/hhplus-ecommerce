package hhplus.ecommerce.unitTest.order.application;

import hhplus.ecommerce.common.presentation.response.PageResponse;
import hhplus.ecommerce.order.application.service.OrderService;
import hhplus.ecommerce.order.application.usecase.*;
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

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private CreateOrderUseCase createOrderUseCase;

    @Mock
    private GetOrderListUseCase getOrderListUseCase;

    @Mock
    private GetOrderDetailUseCase getOrderDetailUseCase;

    @Mock
    private ChangeOrderStatusUseCase changeOrderStatusUseCase;

    @Mock
    private ChangeOrderItemStatusUseCase changeOrderItemStatusUseCase;

    @InjectMocks
    private OrderService orderService;

    @Test
    @DisplayName("주문을 생성할 수 있다")
    void createOrder() {
        // given
        Long userId = 1L;
        OrderCreateRequest request = new OrderCreateRequest();
        OrderItemRequest itemRequest = new OrderItemRequest();
        itemRequest.setProductOptionId(1L);
        itemRequest.setQuantity(2);
        request.setItems(List.of(itemRequest));
        request.setUsedPoints(1000L);

        OrderCreateResponse expectedResponse = new OrderCreateResponse(
                1L,
                "ORD20251107001",
                OrderStatus.PENDING,
                20000L,
                2000L,
                17000L,
                1000L,
                LocalDateTime.now().plusMinutes(15)
        );

        when(createOrderUseCase.execute(eq(userId), any(OrderCreateRequest.class)))
                .thenReturn(expectedResponse);

        // when
        OrderCreateResponse result = orderService.createOrder(userId, request);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getOrderId()).isEqualTo(1L);
        assertThat(result.getOrderStatus()).isEqualTo(OrderStatus.PENDING);
        verify(createOrderUseCase, times(1)).execute(eq(userId), any(OrderCreateRequest.class));
    }

    @Test
    @DisplayName("주문 목록을 조회할 수 있다")
    void getOrderList() {
        // given
        Long userId = 1L;
        OrderStatus status = OrderStatus.PENDING;
        int page = 0;
        int size = 10;

        List<OrderListResponse> content = List.of(
                new OrderListResponse(1L, "ORD001", OrderStatus.PENDING, 20000L,
                        LocalDateTime.now(), LocalDateTime.now().plusMinutes(15))
        );
        PageResponse<OrderListResponse> expectedResponse = new PageResponse<>(content, page, size, 1, 1);

        when(getOrderListUseCase.execute(userId, status, page, size))
                .thenReturn(expectedResponse);

        // when
        PageResponse<OrderListResponse> result = orderService.getOrderList(userId, status, page, size);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        verify(getOrderListUseCase, times(1)).execute(userId, status, page, size);
    }

    @Test
    @DisplayName("주문 상세를 조회할 수 있다")
    void getOrderDetail() {
        // given
        Long orderId = 1L;
        List<OrderItemResponse> items = List.of(
                new OrderItemResponse(1L, "테스트 상품", "옵션1", 2, 10000L, OrderItemStatus.PREPARING)
        );
        OrderDetailResponse expectedResponse = new OrderDetailResponse(
                1L,
                "ORD001",
                OrderStatus.PENDING,
                20000L,
                2000L,
                18000L,
                0L,
                items,
                LocalDateTime.now(),
                LocalDateTime.now().plusMinutes(15)
        );

        when(getOrderDetailUseCase.execute(orderId)).thenReturn(expectedResponse);

        // when
        OrderDetailResponse result = orderService.getOrderDetail(orderId);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getOrderId()).isEqualTo(1L);
        assertThat(result.getItems()).hasSize(1);
        verify(getOrderDetailUseCase, times(1)).execute(orderId);
    }

    @Test
    @DisplayName("주문 상태를 변경할 수 있다")
    void changeOrderStatus() {
        // given
        Long userId = 1L;
        OrderStatusChangeRequest request = new OrderStatusChangeRequest(1L, OrderStatus.PAID);

        List<OrderItemResponse> items = List.of(
                new OrderItemResponse(1L, "테스트 상품", "옵션1", 2, 10000L, OrderItemStatus.PREPARING)
        );
        OrderDetailResponse expectedResponse = new OrderDetailResponse(
                1L,
                "ORD001",
                OrderStatus.PAID,
                20000L,
                2000L,
                18000L,
                0L,
                items,
                LocalDateTime.now(),
                LocalDateTime.now().plusMinutes(15)
        );

        when(changeOrderStatusUseCase.execute(eq(userId), any(OrderStatusChangeRequest.class)))
                .thenReturn(expectedResponse);

        // when
        OrderDetailResponse result = orderService.changeOrderStatus(userId, request);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getOrderStatus()).isEqualTo(OrderStatus.PAID);
        verify(changeOrderStatusUseCase, times(1))
                .execute(eq(userId), any(OrderStatusChangeRequest.class));
    }

    @Test
    @DisplayName("주문 아이템 상태를 변경할 수 있다")
    void changeOrderItemStatus() {
        // given
        Long orderItemId = 1L;
        OrderItemStatusChangeRequest request = new OrderItemStatusChangeRequest();
        request.setStatus(OrderItemStatus.SHIPPING);

        OrderItemResponse expectedResponse = new OrderItemResponse(
                1L,
                "테스트 상품",
                "옵션1",
                2,
                10000L,
                OrderItemStatus.SHIPPING
        );

        when(changeOrderItemStatusUseCase.execute(eq(orderItemId), any(OrderItemStatusChangeRequest.class)))
                .thenReturn(expectedResponse);

        // when
        OrderItemResponse result = orderService.changeOrderItemStatus(orderItemId, request);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getItemStatus()).isEqualTo(OrderItemStatus.SHIPPING);
        verify(changeOrderItemStatusUseCase, times(1))
                .execute(eq(orderItemId), any(OrderItemStatusChangeRequest.class));
    }
}
