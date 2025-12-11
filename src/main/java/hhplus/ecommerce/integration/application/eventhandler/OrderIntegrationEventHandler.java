package hhplus.ecommerce.integration.application.eventhandler;

import hhplus.ecommerce.common.event.EventPublisher;
import hhplus.ecommerce.integration.application.service.ExternalIntegrationService;
import hhplus.ecommerce.integration.domain.event.OrderIntegrationFailedEvent;
import hhplus.ecommerce.order.application.service.OrderService;
import hhplus.ecommerce.order.domain.event.OrderCreatedEvent;
import hhplus.ecommerce.order.domain.model.Order;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 주문 외부 연동 이벤트 핸들러
 *
 * 주문 생성 완료 이벤트를 수신하여 외부 시스템(ERP)에 주문 정보 전송
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderIntegrationEventHandler {

    private final ExternalIntegrationService externalIntegrationService;
    private final OrderService orderService;
    private final EventPublisher eventPublisher;

    /**
     * 주문 생성 완료 이벤트 핸들러
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleOrderCreated(OrderCreatedEvent event) {
        long orderId = event.getOrderId();
        System.out.println("=== 이벤트 핸들러 호출됨: OrderIntegrationEventHandler.handleOrderCreated ===");
        log.info("=== 외부 연동 이벤트 수신 - OrderId: {} ===", orderId);

        try {
            log.info("외부 시스템 연동 시작 - OrderId: {}", orderId);
            Order order = orderService.getOrder(orderId);
            externalIntegrationService.sendOrderToERP(order);
            log.info("외부 시스템 연동 완료 - OrderId: {}", orderId);
        } catch (Exception e) {
            log.error("외부 시스템 연동 실패 - OrderId: {}, 보상 이벤트 발행", orderId, e);
            eventPublisher.publish(new OrderIntegrationFailedEvent(orderId, event.getOrderNumber(), "외부 시스템 연동 중 예외 발생", e));
        }
    }
}
