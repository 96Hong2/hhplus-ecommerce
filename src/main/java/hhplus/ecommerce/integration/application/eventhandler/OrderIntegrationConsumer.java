package hhplus.ecommerce.integration.application.eventhandler;

import hhplus.ecommerce.common.event.EventPublisher;
import hhplus.ecommerce.integration.application.service.ExternalIntegrationService;
import hhplus.ecommerce.integration.domain.event.OrderIntegrationFailedEvent;
import hhplus.ecommerce.order.application.service.OrderService;
import hhplus.ecommerce.order.domain.event.OrderCreatedEvent;
import hhplus.ecommerce.order.domain.model.Order;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderIntegrationConsumer {
    private final ExternalIntegrationService externalIntegrationService;
    private final OrderService orderService;
    private final EventPublisher eventPublisher;

    @KafkaListener(topics = "${app.kafka.topics.order-created}", groupId = "order-integration")
    public void consume(OrderCreatedEvent event, org.springframework.kafka.support.Acknowledgment ack) {
        long orderId = event.getOrderId();
        try {
            log.info("외부 시스템 연동 시작 - OrderId: {}", orderId);

            Order order = orderService.getOrder(orderId);
            externalIntegrationService.sendOrderToERP(order);

            log.info("외부 시스템 연동 완료 - OrderId: {}", orderId);
            ack.acknowledge(); // 수동 커밋
        } catch (Exception e) {
            log.error("외부 시스템 연동 실패 - OrderId: {}, 보상 이벤트 발행", orderId, e);
            eventPublisher.publish(new OrderIntegrationFailedEvent(orderId, event.getOrderNumber(), "외부 시스템 연동 중 예외 발생", e));
            ack.acknowledge(); // 실패 이벤트 발행 후에도 커밋 (재처리 방지)
        }
    }

}
