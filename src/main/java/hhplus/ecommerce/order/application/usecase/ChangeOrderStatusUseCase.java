package hhplus.ecommerce.order.application.usecase;

import hhplus.ecommerce.common.domain.exception.OrderException;
import hhplus.ecommerce.order.application.dto.OrderDetailInfo;
import hhplus.ecommerce.order.application.service.OrderService;
import hhplus.ecommerce.order.domain.model.Order;
import hhplus.ecommerce.order.domain.model.OrderItem;
import hhplus.ecommerce.order.presentation.dto.request.OrderStatusChangeRequest;
import hhplus.ecommerce.order.presentation.dto.response.OrderDetailResponse;
import hhplus.ecommerce.order.presentation.dto.response.OrderItemResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ChangeOrderStatusUseCase {

    private final OrderService orderService;

    /**
     * 주문 상태 변경 UseCase
     */
    public OrderDetailResponse execute(Long userId, OrderStatusChangeRequest request) {
        // 1. 주문 조회
        Order order = orderService.getOrder(request.getOrderId());

        // 2. 권한 검증
        if (!order.getUserId().equals(userId)) {
            throw OrderException.orderCreationFailed("주문을 변경할 권한이 없습니다.");
        }

        // 3. 상태 변경 (Domain 메서드 사용)
        Order updatedOrder = order.updateStatus(request.getOrderStatus());

        // 4. 저장
        orderService.saveOrder(updatedOrder);

        // 5. 주문 아이템 조회
        List<OrderItem> orderItems = orderService.getOrderItems(updatedOrder.getOrderId());

        // 6. Application DTO로 변환
        OrderDetailInfo orderDetailInfo = orderService.toOrderDetailInfo(updatedOrder, orderItems);

        // 7. Presentation DTO로 변환
        return toOrderDetailResponse(orderDetailInfo);
    }

    private OrderDetailResponse toOrderDetailResponse(OrderDetailInfo info) {
        List<OrderItemResponse> itemResponses = info.getOrderItems().stream()
                .map(item -> new OrderItemResponse(
                        item.getOrderItemId(),
                        item.getProductName(),
                        item.getOptionName(),
                        item.getQuantity(),
                        item.getUnitPrice(),
                        item.getItemStatus()
                ))
                .collect(Collectors.toList());

        return new OrderDetailResponse(
                info.getOrderId(),
                info.getOrderNumber(),
                info.getOrderStatus(),
                info.getTotalAmount(),
                info.getDiscountAmount(),
                info.getFinalAmount(),
                itemResponses,
                info.getCreatedAt(),
                info.getExpiresAt()
        );
    }
}
