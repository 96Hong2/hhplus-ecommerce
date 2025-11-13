package hhplus.ecommerce.order.domain.model;

import hhplus.ecommerce.common.domain.constants.BusinessConstants;
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
@Table(name = "orders", indexes = {
    @Index(name = "idx_user_status", columnList = "user_id, order_status"),
    @Index(name = "idx_user_created", columnList = "user_id, created_at"),
    @Index(name = "idx_created_at", columnList = "created_at")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Order {
    private static final String ORDER_NUMBER_PREFIX = "ORD";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long orderId;

    @Column(name = "order_number", nullable = false, unique = true, length = 50)
    private String orderNumber;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "total_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "discount_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal discountAmount;

    @Column(name = "final_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal finalAmount;

    @Column(name = "coupon_id")
    private Long couponId;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", length = 20)
    private PaymentMethod paymentMethod;

    @Enumerated(EnumType.STRING)
    @Column(name = "order_status", nullable = false, length = 20)
    private OrderStatus orderStatus;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    public Order(Long orderId, String orderNumber, Long userId, BigDecimal totalAmount, BigDecimal discountAmount,
                 BigDecimal finalAmount, Long couponId, PaymentMethod paymentMethod,
                 OrderStatus orderStatus, LocalDateTime createdAt, LocalDateTime updatedAt, LocalDateTime expiresAt) {
        this.orderId = orderId;
        this.orderNumber = orderNumber;
        this.userId = userId;
        this.totalAmount = totalAmount;
        this.discountAmount = discountAmount;
        this.finalAmount = finalAmount;
        this.couponId = couponId;
        this.paymentMethod = paymentMethod;
        this.orderStatus = orderStatus;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.expiresAt = expiresAt;
    }

    public Order(Long orderId, String orderNumber, Long userId, BigDecimal totalAmount, BigDecimal discountAmount,
                 BigDecimal finalAmount, Long couponId, PaymentMethod paymentMethod, OrderStatus orderStatus, LocalDateTime expiresAt) {
        this(orderId, orderNumber, userId, totalAmount, discountAmount, finalAmount,
             couponId, paymentMethod, orderStatus, null, null, expiresAt);
    }

    // 편의 생성자 (paymentMethod 없이)
    public Order(Long orderId, String orderNumber, Long userId, BigDecimal totalAmount, BigDecimal discountAmount,
                 BigDecimal finalAmount, Long couponId, OrderStatus orderStatus, LocalDateTime expiresAt) {
        this(orderId, orderNumber, userId, totalAmount, discountAmount, finalAmount,
                couponId, null, orderStatus, null, null, expiresAt);
    }

    public static Order create(String orderNumber, Long userId, BigDecimal totalAmount, BigDecimal discountAmount,
                               Long couponId) {
        validateOrderCreation(orderNumber, userId, totalAmount, discountAmount);

        BigDecimal validDiscountAmount = (discountAmount != null) ? discountAmount : BigDecimal.ZERO;

        BigDecimal finalAmount = calculateFinalAmount(totalAmount, validDiscountAmount);

        // 주문 생성 후 15분 타임아웃 설정
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiresAt = now.plusMinutes(BusinessConstants.STOCK_RESERVATION_TIMEOUT_MINUTES);

        return new Order(
                null, // JPA가 자동 생성
                orderNumber,
                userId,
                totalAmount,
                validDiscountAmount,
                finalAmount,
                couponId,
                null, // 주문 생성 시점에는 결제 수단 미정
                OrderStatus.PENDING,
                now,
                now,
                expiresAt
        );
    }

    private static void validateOrderCreation(String orderNumber, Long userId, BigDecimal totalAmount,
                                             BigDecimal discountAmount) {
        if (orderNumber == null || orderNumber.isEmpty()) {
            throw OrderException.orderCreationFailed("주문번호는 필수값입니다.");
        }

        if (userId == null) {
            throw OrderException.orderCreationFailed("유저ID는 필수값입니다.");
        }

        if (totalAmount == null || totalAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw OrderException.orderCreationFailed("총 합계값이 유효하지 않습니다. : " + totalAmount);
        }

        BigDecimal validDiscountAmount = (discountAmount != null) ? discountAmount : BigDecimal.ZERO;

        if (validDiscountAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw OrderException.orderCreationFailed("할인 금액은 0원 이상이어야 합니다. : " + discountAmount);
        }
    }

    private static BigDecimal calculateFinalAmount(BigDecimal totalAmount, BigDecimal discountAmount) {
        BigDecimal finalAmount = totalAmount.subtract(discountAmount);

        if (finalAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw OrderException.orderCreationFailed("최종 결제 금액이 0원 미만입니다.");
        }

        return finalAmount;
    }

    // orderNumber 생성
    public static String generateOrderNumber() {
        return ORDER_NUMBER_PREFIX + System.currentTimeMillis() + "-" + (int)(Math.random() * 1000);
    }

    // 주문 상태 변경
    public Order updateStatus(OrderStatus newStatus) {
        if (newStatus == null) {
            throw new IllegalArgumentException("주문 상태는 필수입니다.");
        }

        // 동일한 상태로 변경 시도 시 예외 발생
        if (this.orderStatus == newStatus) {
            throw OrderException.invalidOrderStatus(this.orderStatus.name(), newStatus.name());
        }

        return new Order(
                this.orderId,
                this.orderNumber,
                this.userId,
                this.totalAmount,
                this.discountAmount,
                this.finalAmount,
                this.couponId,
                this.paymentMethod,
                newStatus,
                this.createdAt,
                LocalDateTime.now(),
                this.expiresAt
        );
    }

    /**
     * 결제 수단을 설정하고 결제 완료 처리
     * @param paymentMethod 결제 수단
     * @return 결제 완료된 Order 객체
     */
    public Order payWithMethod(PaymentMethod paymentMethod) {
        if (paymentMethod == null) {
            throw new IllegalArgumentException("결제 수단은 필수입니다.");
        }

        if (this.orderStatus == OrderStatus.PAID) {
            throw OrderException.orderAlreadyPaid(this.orderId);
        }

        if (this.orderStatus == OrderStatus.CANCELLED) {
            throw OrderException.orderAlreadyCancelled(this.orderId);
        }

        if (!this.orderStatus.equals(OrderStatus.PENDING)) {
            throw OrderException.invalidOrderStatus(this.orderStatus.name(), OrderStatus.PAID.name());
        }

        // 만료된 주문
        if (isExpired()) {
            throw OrderException.orderTimeout(this.orderId);
        }

        return new Order(
                this.orderId,
                this.orderNumber,
                this.userId,
                this.totalAmount,
                this.discountAmount,
                this.finalAmount,
                this.couponId,
                paymentMethod,
                OrderStatus.PAID,
                this.createdAt,
                LocalDateTime.now(),
                this.expiresAt
        );
    }

    // 결제 완료 처리
    public Order pay() {
        if (this.orderStatus == OrderStatus.PAID) {
            throw OrderException.orderAlreadyPaid(this.orderId);
        }

        if (this.orderStatus == OrderStatus.CANCELLED) {
            throw OrderException.orderAlreadyCancelled(this.orderId);
        }

        if (!this.orderStatus.equals(OrderStatus.PENDING)) {
            throw OrderException.invalidOrderStatus(this.orderStatus.name(), OrderStatus.PAID.name());
        }

        // 만료된 주문
        if (isExpired()) {
            throw OrderException.orderTimeout(this.orderId);
        }

        return updateStatus(OrderStatus.PAID);
    }

    // 주문 취소 처리
    public Order cancel() {
        if (this.orderStatus == OrderStatus.CANCELLED) {
            throw OrderException.orderAlreadyCancelled(this.orderId);
        }

        if (!this.orderStatus.equals(OrderStatus.PENDING)) {
            throw OrderException.orderCancelNotAllowed(this.orderId, this.orderStatus.name());
        }

        return updateStatus(OrderStatus.CANCELLED);
    }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(this.expiresAt);
    }

    public boolean canPayment() {
        return this.orderStatus == OrderStatus.PENDING && !isExpired();
    }

    public boolean isPending() {
        return this.orderStatus == OrderStatus.PENDING;
    }

    public boolean isPaid() {
        return this.orderStatus == OrderStatus.PAID;
    }

    public boolean isCancelled() {
        return this.orderStatus == OrderStatus.CANCELLED;
    }
}
