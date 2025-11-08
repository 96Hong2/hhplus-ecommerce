package hhplus.ecommerce.order.application.usecase;

import hhplus.ecommerce.common.presentation.response.PageResponse;
import hhplus.ecommerce.order.domain.model.Order;
import hhplus.ecommerce.order.domain.model.OrderStatus;
import hhplus.ecommerce.order.domain.repository.OrderRepository;
import hhplus.ecommerce.order.presentation.dto.response.OrderListResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class GetOrderListUseCase {

    private final OrderRepository orderRepository;

    /**
     * 주문 목록 조회 UseCase
     */
    public PageResponse<OrderListResponse> execute(Long userId, OrderStatus status, int page, int size) {
        List<Order> orders;

        // 상태 필터링 적용
        if (status != null) {
            orders = orderRepository.findByUserIdAndStatus(userId, status);
        } else {
            orders = orderRepository.findByUserId(userId);
        }

        // 페이징 처리 (간단한 인메모리 페이징)
        int totalElements = orders.size();
        int totalPages = (int) Math.ceil((double) totalElements / size);
        int start = page * size;
        int end = Math.min(start + size, totalElements);

        List<OrderListResponse> content = orders.stream()
                .skip(start)
                .limit(size)
                .map(order -> new OrderListResponse(
                        order.getOrderId(),
                        order.getOrderNumber(),
                        order.getOrderStatus(),
                        order.getFinalAmount().longValue(),
                        order.getCreatedAt(),
                        order.getExpiresAt()
                ))
                .collect(Collectors.toList());

        return new PageResponse<>(content, page, size, totalElements, totalPages);
    }
}
