package hhplus.ecommerce.order.application.eventhandler;

import hhplus.ecommerce.order.domain.event.OrderCompensatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

/**
 * 주문 알림 처리 Kafka Consumer
 *
 * 보상 완료 이벤트를 수신하여 사용자에게 알림 전송 (간단한 로깅 처리)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationEventConsumer {

    @KafkaListener(topics = "${app.kafka.topics.order-compensated}", groupId = "order-notification")
    public void consume(OrderCompensatedEvent event, Acknowledgment ack) {
        try {
            if (event.isSuccess()) {
                log.info("=== 사용자 알림 발송 ===");
                log.info("주문 취소 알림 - OrderId: {}, OrderNumber: {}",
                        event.getOrderId(), event.getOrderNumber());
                log.info("사유: {}", event.getCompensationReason());
                log.info("======================");
            } else {
                log.error("=== 관리자 알림 발송 ===");
                log.error("보상 트랜잭션 실패 알림 - OrderId: {}, OrderNumber: {}",
                        event.getOrderId(), event.getOrderNumber());
                log.error("사유: {}", event.getCompensationReason());
                log.error("======================");
            }

            ack.acknowledge();
        } catch (Exception e) {
            log.error("알림 발송 실패 - OrderId: {}", event.getOrderId(), e);
            ack.acknowledge(); // 알림 실패는 재시도하지 않음
        }
    }
}
