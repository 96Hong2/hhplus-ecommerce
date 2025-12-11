package hhplus.ecommerce.order.domain.event;

import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 주문 보상 완료 도메인 이벤트
 *
 * 주문 취소 및 재고 복구 등 보상 트랜잭션이 완료된 후 발행되는 이벤트
 * 추가적인 후속 처리(알림, 로깅, 모니터링 등)를 위해 사용 가능
 */
@Getter
public class OrderCompensatedEvent {

    private final Long orderId;
    private final String orderNumber;
    private final String compensationReason;
    private final boolean success;
    private final LocalDateTime occurredAt;

    public OrderCompensatedEvent(Long orderId, String orderNumber,
                                String compensationReason, boolean success) {
        this.orderId = orderId;
        this.orderNumber = orderNumber;
        this.compensationReason = compensationReason;
        this.success = success;
        this.occurredAt = LocalDateTime.now();
    }
}
