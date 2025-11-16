package hhplus.ecommerce.order.application.dto;

import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

/**
 * 주문 생성 커맨드 (Application Layer DTO)
 * 주문 생성에 필요한 모든 정보를 담음
 */
@Getter
public class OrderCreationCommand {
    private final Long userId;
    private final List<OrderItemInfo> orderItems;
    private final BigDecimal totalAmount;
    private final BigDecimal discountAmount;
    private final Long couponId;
    private final String orderNumber;

    public OrderCreationCommand(Long userId, List<OrderItemInfo> orderItems,
                               BigDecimal totalAmount, BigDecimal discountAmount,
                               Long couponId, String orderNumber) {
        this.userId = userId;
        this.orderItems = orderItems;
        this.totalAmount = totalAmount;
        this.discountAmount = discountAmount;
        this.couponId = couponId;
        this.orderNumber = orderNumber;
    }
}
