package hhplus.ecommerce.integration.domain.event;

import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 주문 외부 연동 실패 도메인 이벤트
 *
 * 외부 시스템(ERP 등) 연동 실패 시 발행되는 이벤트
 * 보상 트랜잭션(주문 취소, 재고 복구)을 트리거하기 위해 사용
 */
@Getter
public class OrderIntegrationFailedEvent {

    private final Long orderId;
    private final String orderNumber;
    private final String failureReason;
    private final String exceptionMessage;
    private final String exceptionType;
    private final LocalDateTime occurredAt;

    public OrderIntegrationFailedEvent(Long orderId, String orderNumber,
                                      String failureReason, Throwable exception) {
        this.orderId = orderId;
        this.orderNumber = orderNumber;
        this.failureReason = failureReason;
        this.exceptionMessage = exception != null ? exception.getMessage() : null;
        this.exceptionType = exception != null ? exception.getClass().getSimpleName() : null;
        this.occurredAt = LocalDateTime.now();
    }

    public OrderIntegrationFailedEvent(Long orderId, String orderNumber, String failureReason) {
        this(orderId, orderNumber, failureReason, null);
    }
}
