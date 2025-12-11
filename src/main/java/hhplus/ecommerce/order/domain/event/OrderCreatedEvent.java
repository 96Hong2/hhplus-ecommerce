package hhplus.ecommerce.order.domain.event;

import hhplus.ecommerce.order.application.dto.OrderItemInfo;
import hhplus.ecommerce.order.presentation.dto.response.OrderCreateResponse;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 주문 생성 완료 도메인 이벤트
 *
 * 주문이 성공적으로 생성되고 트랜잭션이 커밋된 후 발행되는 이벤트
 * 외부 시스템 연동(ERP 등)을 트리거하기 위해 사용
 */
@Getter
public class OrderCreatedEvent {

    private final Long orderId;
    private final String orderNumber;
    private final Long userId;
    private final BigDecimal totalAmount;
    private final BigDecimal discountAmount;
    private final BigDecimal finalAmount;
    private final Long couponId;
    private final List<OrderItemInfo> orderItems;
    private final LocalDateTime occurredAt;

    public OrderCreatedEvent(Long orderId, String orderNumber, Long userId,
                           BigDecimal totalAmount, BigDecimal discountAmount, BigDecimal finalAmount,
                           Long couponId, List<OrderItemInfo> orderItems) {
        this.orderId = orderId;
        this.orderNumber = orderNumber;
        this.userId = userId;
        this.totalAmount = totalAmount;
        this.discountAmount = discountAmount;
        this.finalAmount = finalAmount;
        this.couponId = couponId;
        this.orderItems = orderItems;
        this.occurredAt = LocalDateTime.now();
    }

    public static OrderCreatedEvent create(OrderCreateResponse response, Long userId, Long couponId, List<OrderItemInfo> orderItems) {
        return new OrderCreatedEvent(response.getOrderId(), response.getOrderNumber(), userId,
            response.getTotalAmount(),response.getDiscountAmount(), response.getFinalAmount(),
            couponId, orderItems);
    }
}
