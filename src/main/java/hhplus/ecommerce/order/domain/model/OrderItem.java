package hhplus.ecommerce.order.domain.model;

import hhplus.ecommerce.common.domain.exception.OrderException;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "order_items", indexes = {
    @Index(name = "idx_order_id", columnList = "order_id"),
    @Index(name = "idx_order_status", columnList = "order_id, item_status"),
    @Index(name = "idx_item_status", columnList = "item_status"),
    @Index(name = "idx_product_option", columnList = "product_option_id")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long orderItemId;

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "product_option_id", nullable = false)
    private Long productOptionId;

    @Column(name = "product_name", nullable = false, length = 200)
    private String productName;

    @Column(name = "option_name", nullable = false, length = 100)
    private String optionName;

    @Column(name = "unit_price", nullable = false, precision = 15, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "quantity", nullable = false)
    private int quantity;

    @Column(name = "subtotal", nullable = false, precision = 15, scale = 2)
    private BigDecimal subtotal;

    @Enumerated(EnumType.STRING)
    @Column(name = "item_status", nullable = false, length = 20)
    private OrderItemStatus itemStatus;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // 모든 필드를 받는 생성자 (불변 객체 생성용)
    public OrderItem(Long orderItemId, Long orderId, Long productId, Long productOptionId,
                     String productName, String optionName, BigDecimal unitPrice,
                     int quantity, BigDecimal subtotal, OrderItemStatus itemStatus) {
        validateOrderItem(orderId, productId, productOptionId, productName, optionName, unitPrice, quantity);

        this.orderItemId = orderItemId;
        this.orderId = orderId;
        this.productId = productId;
        this.productOptionId = productOptionId;
        this.productName = productName;
        this.optionName = optionName;
        this.unitPrice = unitPrice;
        this.quantity = quantity;
        this.subtotal = subtotal;
        this.itemStatus = itemStatus;
    }

    // 주문 항목 생성 시 사용하는 간편 생성자 (초기 상태는 PREPARING)
    public OrderItem(Long orderId, Long productId, Long productOptionId,
                     String productName, String optionName, BigDecimal unitPrice, int quantity) {
        this(null, orderId, productId, productOptionId, productName, optionName, unitPrice,
             quantity, calculateSubtotal(unitPrice, quantity), OrderItemStatus.PREPARING);
    }

    /**
     * 주문 아이템 생성 팩토리 메서드
     * @param orderId 주문 ID
     * @param productId 상품 ID
     * @param productOptionId 상품 옵션 ID
     * @param productName 상품명
     * @param optionName 옵션명
     * @param unitPrice 단가
     * @param quantity 수량
     * @return 생성된 OrderItem
     */
    public static OrderItem create(Long orderId, Long productId, Long productOptionId,
                                  String productName, String optionName,
                                  BigDecimal unitPrice, int quantity) {
        return new OrderItem(orderId, productId, productOptionId, productName, optionName, unitPrice, quantity);
    }


    /**
     * subtotal 계산 (단가 * 수량)
     */
    private static BigDecimal calculateSubtotal(BigDecimal unitPrice, int quantity) {
        if (unitPrice == null) {
            throw new IllegalArgumentException("단가는 필수입니다");
        }
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }

    /**
     * subtotal 조회 (현재 객체의 subtotal)
     */
    public BigDecimal getSubtotal() {
        return this.subtotal;
    }

    // 주문 항목 생성 시 필수 필드 검증
    private void validateOrderItem(Long orderId, Long productId, Long productOptionId,
                                   String productName, String optionName,
                                   BigDecimal unitPrice, int quantity) {
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
        if (unitPrice == null || unitPrice.compareTo(BigDecimal.ZERO) < 0) {
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
            this.unitPrice,
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