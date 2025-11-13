package hhplus.ecommerce.order.application.usecase;

import hhplus.ecommerce.order.application.dto.OrderItemDetailInfo;
import hhplus.ecommerce.order.application.service.OrderService;
import hhplus.ecommerce.order.domain.model.OrderItem;
import hhplus.ecommerce.order.presentation.dto.request.OrderItemStatusChangeRequest;
import hhplus.ecommerce.order.presentation.dto.response.OrderItemResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ChangeOrderItemStatusUseCase {

    private final OrderService orderService;

    /**
     * 주문 아이템 상태 변경 UseCase
     */
    public OrderItemResponse execute(Long orderItemId, OrderItemStatusChangeRequest request) {
        // 1. 주문 아이템 조회
        OrderItem orderItem = orderService.getOrderItem(orderItemId);

        // 2. 상태 변경
        OrderItem updatedOrderItem = orderItem.changeStatus(request.getStatus());

        // 3. 저장
        orderService.saveOrderItem(updatedOrderItem);

        // 4. Application DTO로 변환
        OrderItemDetailInfo itemDetailInfo = orderService.toOrderItemDetailInfo(updatedOrderItem);

        // 5. Presentation DTO로 변환
        return toOrderItemResponse(itemDetailInfo);
    }

    // Application DTO -> Presentation DTO 변환
    private OrderItemResponse toOrderItemResponse(OrderItemDetailInfo info) {
        return new OrderItemResponse(
                info.getOrderItemId(),
                info.getProductName(),
                info.getOptionName(),
                info.getQuantity(),
                info.getUnitPrice(),
                info.getItemStatus()
        );
    }
}
