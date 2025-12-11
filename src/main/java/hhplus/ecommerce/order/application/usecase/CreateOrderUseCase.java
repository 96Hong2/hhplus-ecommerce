package hhplus.ecommerce.order.application.usecase;

import hhplus.ecommerce.common.event.EventPublisher;
import hhplus.ecommerce.order.application.dto.OrderItemInfo;
import hhplus.ecommerce.order.application.service.OrderService;
import hhplus.ecommerce.order.domain.event.OrderCreatedEvent;
import hhplus.ecommerce.order.domain.model.Order;
import hhplus.ecommerce.order.presentation.dto.request.OrderCreateRequest;
import hhplus.ecommerce.order.presentation.dto.response.OrderCreateResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * 주문 생성 UseCase - 이벤트 기반 코레오그래피 패턴
 *
 * 이벤트 기반으로 관심사를 분리:
 * 1. 주문 생성 트랜잭션 (메인 책임)
 * 2. OrderCreatedEvent 발행 → 외부 시스템 연동은 이벤트 핸들러가 처리
 * 3. 보상 트랜잭션도 이벤트 핸들러가 자율적으로 처리
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CreateOrderUseCase {

    private final OrderService orderService;
    private final EventPublisher eventPublisher;

    /**
     * 주문 생성 실행
     *
     * 이벤트 기반 코레오그래피 패턴:
     * 1. 주문 생성 트랜잭션 + OrderCreatedEvent 발행 (트랜잭션 안)
     * 2. OrderIntegrationEventHandler가 외부 시스템(ERP) 연동 (트랜잭션 커밋 후)
     * 3. 외부 연동 실패 시 OrderCompensationEventHandler가 보상 처리
     *
     * @param userId 사용자 ID
     * @param request 주문 생성 요청
     * @return 주문 생성 응답
     */
    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public OrderCreateResponse execute(Long userId, OrderCreateRequest request) {
        log.info("주문 생성 시작 - UserId: {}", userId);

        // 1. 주문 아이템 정보 수집
        List<OrderItemInfo> orderItemInfos = orderService.collectOrderItemsBatch(request.getItems());

        // 2. 총 주문 금액 계산
        BigDecimal totalAmount = orderService.calculateTotalAmount(orderItemInfos);

        // 3. 쿠폰 할인 금액 계산
        BigDecimal discountAmount = orderService.calculateCouponDiscount(
                request.getCouponId(),
                totalAmount
        );

        // 4. 주문 번호 생성
        String orderNumber = orderService.generateOrderNumber(userId);

        // 5. 주문 생성
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

        // 9. 응답 생성
        OrderCreateResponse response = new OrderCreateResponse(
                savedOrder.getOrderId(),
                savedOrder.getOrderNumber(),
                savedOrder.getOrderStatus(),
                savedOrder.getTotalAmount(),
                savedOrder.getDiscountAmount(),
                savedOrder.getFinalAmount(),
                savedOrder.getExpiresAt()
        );

        // 10. 주문 생성 완료 이벤트 발행 (트랜잭션 커밋 후 핸들러 실행)
        eventPublisher.publish(OrderCreatedEvent.create(
                response,
                userId,
                request.getCouponId(),
                orderItemInfos
        ));

        log.info("주문 생성 완료 - OrderId: {}, OrderNumber: {}",
                response.getOrderId(), response.getOrderNumber());

        return response;
    }

}
