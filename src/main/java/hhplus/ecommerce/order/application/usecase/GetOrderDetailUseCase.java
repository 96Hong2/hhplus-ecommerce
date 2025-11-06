package hhplus.ecommerce.order.application.usecase;

import hhplus.ecommerce.common.domain.exception.OrderException;
import hhplus.ecommerce.order.domain.model.Order;
import hhplus.ecommerce.order.domain.model.OrderItem;
import hhplus.ecommerce.order.domain.repository.OrderItemRepository;
import hhplus.ecommerce.order.domain.repository.OrderRepository;
import hhplus.ecommerce.order.presentation.dto.response.OrderDetailResponse;
import hhplus.ecommerce.order.presentation.dto.response.OrderItemResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class GetOrderDetailUseCase {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;

    /**
     * 주문 상세 조회 UseCase
     */
    public OrderDetailResponse execute(Long orderId) {
        // 주문 조회
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> OrderException.orderNotFound(orderId));

        // 주문 아이템 목록 조회
        List<OrderItem> orderItems = orderItemRepository.findAllByOrderId(orderId);

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
                order.getOrderId(),
                order.getOrderNumber(),
                order.getOrderStatus(),
                order.getTotalAmount().longValue(),
                order.getDiscountAmount().longValue(),
                order.getFinalAmount().longValue(),
                order.getUsedPoints().longValue(),
                itemResponses,
                order.getCreatedAt(),
                order.getExpiresAt()
        );
    }
}
