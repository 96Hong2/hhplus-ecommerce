package hhplus.ecommerce.order.application.dto;

import lombok.Getter;

import java.math.BigDecimal;

/**
 * 주문 아이템 정보 (Application Layer DTO)
 * UseCase와 Service 간 데이터 전달에 사용
 */
@Getter
public class OrderItemInfo {
    private final Long productId;
    private final Long productOptionId;
    private final String productName;
    private final String optionName;
    private final int quantity;
    private final BigDecimal unitPrice;
    private final BigDecimal subtotal;

    public OrderItemInfo(Long productId, Long productOptionId, String productName,
                        String optionName, int quantity, BigDecimal unitPrice, BigDecimal subtotal) {
        this.productId = productId;
        this.productOptionId = productOptionId;
        this.productName = productName;
        this.optionName = optionName;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.subtotal = subtotal;
    }
}
