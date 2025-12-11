package hhplus.ecommerce.order.application.eventhandler;

import hhplus.ecommerce.common.event.EventPublisher;
import hhplus.ecommerce.integration.domain.event.OrderIntegrationFailedEvent;
import hhplus.ecommerce.order.application.service.OrderCompensationService;
import hhplus.ecommerce.order.domain.event.OrderCompensatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 주문 보상 이벤트 핸들러
 *
 * 외부 연동 실패 이벤트를 수신하여 보상 트랜잭션(주문 취소, 재고 복구) 실행
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderCompensationEventHandler {

    private final OrderCompensationService orderCompensationService;
    private final EventPublisher eventPublisher;

    /**
     * 외부 연동 실패 이벤트 핸들러
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleIntegrationFailed(OrderIntegrationFailedEvent event) {
        log.info("외부 연동 실패 이벤트 수신 - OrderId: {}, Reason: {}",
                event.getOrderId(), event.getFailureReason());

        try {
            // 보상 트랜잭션 실행
            orderCompensationService.compensateOrder(event.getOrderId());

            // 보상 완료 이벤트 발행
            eventPublisher.publish(new OrderCompensatedEvent(
                    event.getOrderId(),
                    event.getOrderNumber(),
                    event.getFailureReason(),
                    true
            ));

            log.info("보상 트랜잭션 완료 이벤트 발행 - OrderId: {}", event.getOrderId());
        } catch (Exception e) {
            log.error("보상 트랜잭션 실패 - OrderId: {}", event.getOrderId(), e);

            // 보상 실패 이벤트 발행
            eventPublisher.publish(new OrderCompensatedEvent(
                    event.getOrderId(),
                    event.getOrderNumber(),
                    "보상 트랜잭션 실패: " + e.getMessage(),
                    false
            ));

            throw e;
        }
    }
}
