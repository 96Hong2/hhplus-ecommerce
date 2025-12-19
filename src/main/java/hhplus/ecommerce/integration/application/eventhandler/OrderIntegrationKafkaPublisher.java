package hhplus.ecommerce.integration.application.eventhandler;

import hhplus.ecommerce.order.domain.event.OrderCreatedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 주문 외부 연동 이벤트 핸들러
 *
 * 주문 생성 완료 이벤트를 수신하여 외부 시스템(ERP)에 주문 정보 전송
 */
@Slf4j
@Component
public class OrderIntegrationKafkaPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final String topic;

    public OrderIntegrationKafkaPublisher (
        KafkaTemplate<String, Object> kafkaTemplate,
        @Value("${app.kafka.topics.order-created}") String topic
    ) {
        this.kafkaTemplate = kafkaTemplate;
        this.topic = topic;
    }

    /**
     * 주문 생성 완료 이벤트 핸들러
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleOrderCreated(OrderCreatedEvent event) {
        kafkaTemplate.send(topic, String.valueOf(event.getOrderId()), event); // 토픽, 키, 데이터
    }
}
