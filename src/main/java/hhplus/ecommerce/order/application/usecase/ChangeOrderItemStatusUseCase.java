package hhplus.ecommerce.order.application.usecase;

import hhplus.ecommerce.common.domain.exception.OrderException;
import hhplus.ecommerce.order.domain.model.OrderItem;
import hhplus.ecommerce.order.domain.repository.OrderItemRepository;
import hhplus.ecommerce.order.presentation.dto.request.OrderItemStatusChangeRequest;
import hhplus.ecommerce.order.presentation.dto.response.OrderItemResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ChangeOrderItemStatusUseCase {

    private final OrderItemRepository orderItemRepository;

    /**
     * 주문 아이템 상태 변경 UseCase
     */
    public OrderItemResponse execute(Long orderItemId, OrderItemStatusChangeRequest request) {
        OrderItem orderItem = orderItemRepository.findById(orderItemId)
                .orElseThrow(() -> OrderException.orderItemNotFound(orderItemId));

        OrderItem updatedOrderItem = orderItem.changeStatus(request.getStatus());
        orderItemRepository.save(updatedOrderItem);

        return new OrderItemResponse(
                updatedOrderItem.getOrderItemId(),
                updatedOrderItem.getProductName(),
                updatedOrderItem.getOptionName(),
                updatedOrderItem.getQuantity(),
                updatedOrderItem.getProductPrice().longValue(),
                updatedOrderItem.getItemStatus()
        );
    }
}
