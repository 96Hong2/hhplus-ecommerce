package hhplus.ecommerce.order.application.usecase;

import hhplus.ecommerce.common.presentation.response.PageResponse;
import hhplus.ecommerce.order.application.service.OrderService;
import hhplus.ecommerce.order.domain.model.Order;
import hhplus.ecommerce.order.domain.model.OrderStatus;
import hhplus.ecommerce.order.presentation.dto.response.OrderListResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class GetOrderListUseCase {

    private final OrderService orderService;

    /**
     * 주문 목록 조회 UseCase - 전체 흐름을 제어
     * 1. 주문 목록 조회 (Service)
     * 2. 페이징 처리 (UseCase)
     * 3. Presentation DTO로 변환 (UseCase)
     */
    public PageResponse<OrderListResponse> execute(Long userId, OrderStatus status, int page, int size) {
        // 1. 주문 목록 조회 (상태 필터링 포함)
        List<Order> orders = orderService.getOrdersByUserId(userId, status);

        // 2. 페이징 처리 (간단한 인메모리 페이징)
        int totalElements = orders.size();
        int totalPages = (int) Math.ceil((double) totalElements / size);
        int start = page * size;
        int end = Math.min(start + size, totalElements);

        // 3. Presentation DTO로 변환
        List<OrderListResponse> content = orders.stream()
                .skip(start)
                .limit(size)
                .map(order -> new OrderListResponse(
                        order.getOrderId(),
                        order.getOrderNumber(),
                        order.getOrderStatus(),
                        order.getFinalAmount(),
                        order.getCreatedAt(),
                        order.getExpiresAt()
                ))
                .collect(Collectors.toList());

        return new PageResponse<>(content, page, size, totalElements, totalPages);
    }
}
