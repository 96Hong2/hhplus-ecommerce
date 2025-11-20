package hhplus.ecommerce.order.application.dto;

import hhplus.ecommerce.order.domain.model.OrderStatus;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 주문 상세 정보 (Application Layer DTO)
 * 주문과 주문 아이템 정보를 함께 포함
 */
@Getter
public class OrderDetailInfo {
    private final Long orderId;
    private final String orderNumber;
    private final OrderStatus orderStatus;
    private final BigDecimal totalAmount;
    private final BigDecimal discountAmount;
    private final BigDecimal finalAmount;
    private final List<OrderItemDetailInfo> orderItems;
    private final LocalDateTime createdAt;
    private final LocalDateTime expiresAt;

    public OrderDetailInfo(Long orderId, String orderNumber, OrderStatus orderStatus,
                          BigDecimal totalAmount, BigDecimal discountAmount, BigDecimal finalAmount,
                          List<OrderItemDetailInfo> orderItems,
                          LocalDateTime createdAt, LocalDateTime expiresAt) {
        this.orderId = orderId;
        this.orderNumber = orderNumber;
        this.orderStatus = orderStatus;
        this.totalAmount = totalAmount;
        this.discountAmount = discountAmount;
        this.finalAmount = finalAmount;
        this.orderItems = orderItems;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
    }
}
