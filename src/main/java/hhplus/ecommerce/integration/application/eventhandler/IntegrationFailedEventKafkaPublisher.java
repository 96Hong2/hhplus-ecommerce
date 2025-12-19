package hhplus.ecommerce.integration.application.eventhandler;

import hhplus.ecommerce.integration.domain.event.OrderIntegrationFailedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 외부 연동 실패 이벤트 Kafka Publisher
 *
 * 외부 연동 실패 이벤트를 Kafka로 발행하여 보상 트랜잭션 트리거
 */
@Slf4j
@Component
public class IntegrationFailedEventKafkaPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final String topic;

    public IntegrationFailedEventKafkaPublisher(
        KafkaTemplate<String, Object> kafkaTemplate,
        @Value("${app.kafka.topics.order-integration-failed}") String topic
    ) {
        this.kafkaTemplate = kafkaTemplate;
        this.topic = topic;
    }

    /**
     * 외부 연동 실패 이벤트를 Kafka로 발행
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleIntegrationFailed(OrderIntegrationFailedEvent event) {
        log.info("외부 연동 실패 이벤트 Kafka 발행 - OrderId: {}", event.getOrderId());
        kafkaTemplate.send(topic, String.valueOf(event.getOrderId()), event);
    }
}
