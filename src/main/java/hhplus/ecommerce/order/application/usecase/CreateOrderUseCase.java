package hhplus.ecommerce.order.application.usecase;

import hhplus.ecommerce.order.application.dto.OrderItemInfo;
import hhplus.ecommerce.order.application.service.OrderService;
import hhplus.ecommerce.order.domain.model.Order;
import hhplus.ecommerce.order.presentation.dto.request.OrderCreateRequest;
import hhplus.ecommerce.order.presentation.dto.response.OrderCreateResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Component
@RequiredArgsConstructor
public class CreateOrderUseCase {

    private final OrderService orderService;

    /**
     * 주문 생성 UseCase - 전체 흐름을 제어
     * 1. 주문 아이템 정보 수집 (Service)
     * 2. 총 금액 계산 (Service)
     * 3. 쿠폰 할인 계산 (Service)
     * 4. 주문 생성 (Domain)
     * 5. 재고 예약 (Service)
     * 6. 주문 아이템 저장 (Service)
     *
     * @Transactional: 주문 저장, 재고 예약, 주문 아이템 저장이 모두 성공하거나 모두 롤백되어야 함
     */
    @Transactional
    public OrderCreateResponse execute(Long userId, OrderCreateRequest request) {
        // 1. 주문 아이템 정보 수집 (상품 조회, 가격 계산 포함)
        List<OrderItemInfo> orderItemInfos = orderService.collectOrderItems(request.getItems());

        // 2. 총 주문 금액 계산
        BigDecimal totalAmount = orderService.calculateTotalAmount(orderItemInfos);

        // 3. 쿠폰 할인 금액 계산
        BigDecimal discountAmount = orderService.calculateCouponDiscount(
                request.getCouponId(),
                totalAmount
        );

        // 4. 주문 번호 생성
        String orderNumber = orderService.generateOrderNumber(userId);

        // 5. 주문 생성 (Domain 팩토리 메서드 사용)
        Order order = Order.create(
                orderNumber,
                userId,
                totalAmount,
                discountAmount,
                request.getCouponId()
        );

        // 6. 주문 저장
        Order savedOrder = orderService.saveOrder(order);

        // 7. 재고 예약
        orderService.reserveStocks(savedOrder.getOrderId(), orderItemInfos);

        // 8. 주문 아이템 생성 및 저장
        orderService.saveOrderItems(savedOrder.getOrderId(), orderItemInfos);

        // 9. 응답 생성 (Presentation DTO로 변환)
        return new OrderCreateResponse(
                savedOrder.getOrderId(),
                savedOrder.getOrderNumber(),
                savedOrder.getOrderStatus(),
                savedOrder.getTotalAmount(),
                savedOrder.getDiscountAmount(),
                savedOrder.getFinalAmount(),
                savedOrder.getExpiresAt()
        );
    }
}
