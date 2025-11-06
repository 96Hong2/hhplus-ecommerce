package hhplus.ecommerce.order.domain.model;

import hhplus.ecommerce.common.domain.exception.OrderException;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
public class OrderItem {

    private final Long orderItemId;
    private final Long orderId;
    private final Long productId;
    private final Long productOptionId;
    private final String productName;
    private final String optionName;
    private final BigDecimal productPrice;
    private final int quantity;
    private final BigDecimal subtotal;
    private final OrderItemStatus itemStatus;

    // 모든 필드를 받는 생성자 (불변 객체 생성용)
    public OrderItem(Long orderItemId, Long orderId, Long productId, Long productOptionId,
                     String productName, String optionName, BigDecimal productPrice,
                     int quantity, BigDecimal subtotal, OrderItemStatus itemStatus) {
        validateOrderItem(orderId, productId, productOptionId, productName, optionName, productPrice, quantity);

        this.orderItemId = orderItemId;
        this.orderId = orderId;
        this.productId = productId;
        this.productOptionId = productOptionId;
        this.productName = productName;
        this.optionName = optionName;
        this.productPrice = productPrice;
        this.quantity = quantity;
        this.subtotal = subtotal;
        this.itemStatus = itemStatus;
    }

    // 주문 항목 생성 시 사용하는 간편 생성자 (초기 상태는 PREPARING)
    public OrderItem(Long orderId, Long productId, Long productOptionId,
                     String productName, String optionName, BigDecimal productPrice, int quantity) {
        this(null, orderId, productId, productOptionId, productName, optionName, productPrice,
             quantity, productPrice.multiply(BigDecimal.valueOf(quantity)), OrderItemStatus.PREPARING);
    }

    // 주문 항목 생성 시 필수 필드 검증
    private void validateOrderItem(Long orderId, Long productId, Long productOptionId,
                                   String productName, String optionName,
                                   BigDecimal productPrice, int quantity) {
        if (orderId == null) {
            throw OrderException.orderCreationFailed("주문 ID는 필수입니다");
        }
        if (productId == null) {
            throw OrderException.orderCreationFailed("상품 ID는 필수입니다");
        }
        if (productOptionId == null) {
            throw OrderException.orderCreationFailed("상품 옵션 ID는 필수입니다");
        }
        if (productName == null || productName.trim().isEmpty()) {
            throw OrderException.orderCreationFailed("상품명은 필수입니다");
        }
        if (optionName == null || optionName.trim().isEmpty()) {
            throw OrderException.orderCreationFailed("옵션명은 필수입니다");
        }
        if (productPrice == null || productPrice.compareTo(BigDecimal.ZERO) < 0) {
            throw OrderException.orderCreationFailed("상품 가격은 0 이상이어야 합니다");
        }
        if (quantity <= 0) {
            throw OrderException.orderCreationFailed("수량은 1 이상이어야 합니다");
        }
    }

    // 주문 항목 상태 변경 (API 명세: PREPARING 상태에서만 CANCELLED 가능)
    public OrderItem changeStatus(OrderItemStatus newStatus) {
        if (newStatus == null) {
            throw OrderException.invalidOrderItemStatus(
                this.itemStatus.name(), "NULL"
            );
        }

        // CANCELLED는 PREPARING 상태에서만 가능
        if (newStatus == OrderItemStatus.CANCELLED && this.itemStatus != OrderItemStatus.PREPARING) {
            throw OrderException.invalidOrderItemStatus(
                this.itemStatus.name(), newStatus.name()
            );
        }

        if (this.itemStatus == newStatus) {
            return this;
        }

        return new OrderItem(
            this.orderItemId,
            this.orderId,
            this.productId,
            this.productOptionId,
            this.productName,
            this.optionName,
            this.productPrice,
            this.quantity,
            this.subtotal,
            newStatus
        );
    }

    public OrderItem cancel() {
        if (!canBeCancelled()) {
            throw OrderException.invalidOrderItemStatus(
                this.itemStatus.name(), OrderItemStatus.CANCELLED.name()
            );
        }
        return changeStatus(OrderItemStatus.CANCELLED);
    }

    public OrderItem startShipping() {
        if (this.itemStatus != OrderItemStatus.PREPARING) {
            throw OrderException.invalidOrderItemStatus(
                this.itemStatus.name(), OrderItemStatus.SHIPPING.name()
            );
        }
        return changeStatus(OrderItemStatus.SHIPPING);
    }

    public OrderItem completeDelivery() {
        if (this.itemStatus != OrderItemStatus.SHIPPING) {
            throw OrderException.invalidOrderItemStatus(
                this.itemStatus.name(), OrderItemStatus.DELIVERED.name()
            );
        }
        return changeStatus(OrderItemStatus.DELIVERED);
    }

    public boolean canBeCancelled() {
        return this.itemStatus == OrderItemStatus.PREPARING;
    }

    public boolean canStartShipping() {
        return this.itemStatus == OrderItemStatus.PREPARING;
    }

    public boolean canCompleteDelivery() {
        return this.itemStatus == OrderItemStatus.SHIPPING;
    }
}