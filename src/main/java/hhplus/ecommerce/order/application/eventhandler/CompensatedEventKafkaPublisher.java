package hhplus.ecommerce.order.application.eventhandler;

import hhplus.ecommerce.order.domain.event.OrderCompensatedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 보상 완료 이벤트 Kafka Publisher
 *
 * 보상 트랜잭션 완료 이벤트를 Kafka로 발행하여 알림 처리 트리거
 */
@Slf4j
@Component
public class CompensatedEventKafkaPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final String topic;

    public CompensatedEventKafkaPublisher(
        KafkaTemplate<String, Object> kafkaTemplate,
        @Value("${app.kafka.topics.order-compensated}") String topic
    ) {
        this.kafkaTemplate = kafkaTemplate;
        this.topic = topic;
    }

    /**
     * 보상 완료 이벤트를 Kafka로 발행
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleOrderCompensated(OrderCompensatedEvent event) {
        log.info("보상 완료 이벤트 Kafka 발행 - OrderId: {}, Success: {}",
                event.getOrderId(), event.isSuccess());
        kafkaTemplate.send(topic, String.valueOf(event.getOrderId()), event);
    }
}
