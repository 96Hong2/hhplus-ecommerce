package hhplus.ecommerce.order.application.eventhandler;

import hhplus.ecommerce.order.domain.event.OrderCompensatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 주문 알림 이벤트 핸들러
 *
 * 보상 완료 이벤트를 수신하여 사용자에게 알림 전송 (간단한 로깅 처리)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderNotificationEventHandler {

    /**
     * 보상 완료 이벤트 핸들러 (후처리)
     *
     * 실제로는 SMS, 푸시 알림, 이메일 등을 전송하지만
     * 여기서는 간단히 로깅으로 처리
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleOrderCompensated(OrderCompensatedEvent event) {
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
    }
}
