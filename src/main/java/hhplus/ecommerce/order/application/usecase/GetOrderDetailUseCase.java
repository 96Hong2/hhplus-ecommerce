package hhplus.ecommerce.order.application.usecase;

import hhplus.ecommerce.order.application.dto.OrderDetailInfo;
import hhplus.ecommerce.order.application.service.OrderService;
import hhplus.ecommerce.order.domain.model.Order;
import hhplus.ecommerce.order.domain.model.OrderItem;
import hhplus.ecommerce.order.presentation.dto.response.OrderDetailResponse;
import hhplus.ecommerce.order.presentation.dto.response.OrderItemResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class GetOrderDetailUseCase {

    private final OrderService orderService;

    /**
     * 주문 상세 조회 UseCase - 전체 흐름을 제어
     * 1. 주문 조회 (Service)
     * 2. 주문 아이템 조회 (Service)
     * 3. Application DTO로 변환 (Service)
     * 4. Presentation DTO로 변환 (UseCase)
     */
    public OrderDetailResponse execute(Long orderId) {
        // 1. 주문 조회
        Order order = orderService.getOrder(orderId);

        // 2. 주문 아이템 조회
        List<OrderItem> orderItems = orderService.getOrderItems(orderId);

        // 3. Application DTO로 변환
        OrderDetailInfo orderDetailInfo = orderService.toOrderDetailInfo(order, orderItems);

        // 4. Presentation DTO로 변환
        return toOrderDetailResponse(orderDetailInfo);
    }

    private OrderDetailResponse toOrderDetailResponse(OrderDetailInfo info) {
        List<OrderItemResponse> itemResponses = info.getOrderItems().stream()
                .map(item -> new OrderItemResponse(
                        item.getOrderItemId(),
                        item.getProductName(),
                        item.getOptionName(),
                        item.getQuantity(),
                        item.getUnitPrice().longValue(),
                        item.getItemStatus()
                ))
                .collect(Collectors.toList());

        return new OrderDetailResponse(
                info.getOrderId(),
                info.getOrderNumber(),
                info.getOrderStatus(),
                info.getTotalAmount().longValue(),
                info.getDiscountAmount().longValue(),
                info.getFinalAmount().longValue(),
                info.getUsedPoints().longValue(),
                itemResponses,
                info.getCreatedAt(),
                info.getExpiresAt()
        );
    }
}
