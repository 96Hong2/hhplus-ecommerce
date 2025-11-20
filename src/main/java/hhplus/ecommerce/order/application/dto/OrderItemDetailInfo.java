package hhplus.ecommerce.order.application.dto;

import hhplus.ecommerce.order.domain.model.OrderItemStatus;
import lombok.Getter;

import java.math.BigDecimal;

/**
 * 주문 아이템 상세 정보 (Application Layer DTO)
 */
@Getter
public class OrderItemDetailInfo {
    private final Long orderItemId;
    private final String productName;
    private final String optionName;
    private final int quantity;
    private final BigDecimal unitPrice;
    private final OrderItemStatus itemStatus;

    public OrderItemDetailInfo(Long orderItemId, String productName, String optionName,
                              int quantity, BigDecimal unitPrice, OrderItemStatus itemStatus) {
        this.orderItemId = orderItemId;
        this.productName = productName;
        this.optionName = optionName;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.itemStatus = itemStatus;
    }
}
