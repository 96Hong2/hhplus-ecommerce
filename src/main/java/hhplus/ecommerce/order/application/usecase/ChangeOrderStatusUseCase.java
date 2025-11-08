package hhplus.ecommerce.order.application.usecase;

import hhplus.ecommerce.common.domain.exception.OrderException;
import hhplus.ecommerce.order.domain.model.Order;
import hhplus.ecommerce.order.domain.model.OrderItem;
import hhplus.ecommerce.order.domain.repository.OrderItemRepository;
import hhplus.ecommerce.order.domain.repository.OrderRepository;
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

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;

    /**
     * 주문 상태 변경 UseCase
     */
    public OrderDetailResponse execute(Long userId, OrderStatusChangeRequest request) {
        Order order = orderRepository.findById(request.getOrderId())
                .orElseThrow(() -> OrderException.orderNotFound(request.getOrderId()));

        if (!order.getUserId().equals(userId)) {
            throw OrderException.orderCreationFailed("주문을 변경할 권한이 없습니다.");
        }

        Order updatedOrder = order.updateStatus(request.getOrderStatus());
        orderRepository.save(updatedOrder);

        // 주문 아이템 목록 조회
        List<OrderItem> orderItems = orderItemRepository.findAllByOrderId(updatedOrder.getOrderId());
        List<OrderItemResponse> itemResponses = orderItems.stream()
                .map(item -> new OrderItemResponse(
                        item.getOrderItemId(),
                        item.getProductName(),
                        item.getOptionName(),
                        item.getQuantity(),
                        item.getProductPrice().longValue(),
                        item.getItemStatus()
                ))
                .collect(Collectors.toList());

        return new OrderDetailResponse(
                updatedOrder.getOrderId(),
                updatedOrder.getOrderNumber(),
                updatedOrder.getOrderStatus(),
                updatedOrder.getTotalAmount().longValue(),
                updatedOrder.getDiscountAmount().longValue(),
                updatedOrder.getFinalAmount().longValue(),
                updatedOrder.getUsedPoints().longValue(),
                itemResponses,
                updatedOrder.getCreatedAt(),
                updatedOrder.getExpiresAt()
        );
    }
}
