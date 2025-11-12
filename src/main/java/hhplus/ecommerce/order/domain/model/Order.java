package hhplus.ecommerce.order.domain.model;

import hhplus.ecommerce.common.domain.constants.BusinessConstants;
import hhplus.ecommerce.common.domain.exception.OrderException;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicLong;

@Getter
public class Order {
    private final Long orderId;
    private final String orderNumber;
    private final Long userId;
    private final BigDecimal totalAmount;
    private final BigDecimal discountAmount;
    private final BigDecimal finalAmount;
    private final BigDecimal usedPoints;
    private final Long couponId;
    private final PaymentMethod paymentMethod; // 결제 수단
    private final OrderStatus orderStatus;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;
    private final LocalDateTime expiresAt;

    private static AtomicLong sequence = new AtomicLong(1);
    private static final String ORDER_NUMBER_PREFIX = "ORD";

    public Order(Long orderId, String orderNumber, Long userId, BigDecimal totalAmount, BigDecimal discountAmount,
                 BigDecimal finalAmount, BigDecimal usedPoints, Long couponId, PaymentMethod paymentMethod,
                 OrderStatus orderStatus, LocalDateTime createdAt, LocalDateTime updatedAt, LocalDateTime expiresAt) {
        this.orderId = orderId;
        this.orderNumber = orderNumber;
        this.userId = userId;
        this.totalAmount = totalAmount;
        this.discountAmount = discountAmount;
        this.finalAmount = finalAmount;
        this.usedPoints = usedPoints;
        this.couponId = couponId;
        this.paymentMethod = paymentMethod;
        this.orderStatus = orderStatus;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.expiresAt = expiresAt;
    }

    public Order(Long orderId, String orderNumber, Long userId, BigDecimal totalAmount, BigDecimal discountAmount,
                 BigDecimal finalAmount, Long couponId, PaymentMethod paymentMethod, OrderStatus orderStatus, LocalDateTime expiresAt) {
        this(orderId, orderNumber, userId, totalAmount, discountAmount, finalAmount, BigDecimal.ZERO,
             couponId, paymentMethod, orderStatus, null, null, expiresAt);
    }

    public static Order create(String orderNumber, Long userId, BigDecimal totalAmount, BigDecimal discountAmount,
                               BigDecimal usedPoints, Long couponId) {
        validateOrderCreation(orderNumber, userId, totalAmount, discountAmount, usedPoints);

        BigDecimal validDiscountAmount = (discountAmount != null) ? discountAmount : BigDecimal.ZERO;
        BigDecimal validUsedPoints = (usedPoints != null) ? usedPoints : BigDecimal.ZERO;

        BigDecimal finalAmount = calculateFinalAmount(totalAmount, validDiscountAmount, validUsedPoints);

        // 주문 생성 후 15분 타임아웃 설정
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiresAt = now.plusMinutes(BusinessConstants.STOCK_RESERVATION_TIMEOUT_MINUTES);

        return new Order(
                sequence.incrementAndGet(),
                orderNumber,
                userId,
                totalAmount,
                validDiscountAmount,
                finalAmount,
                validUsedPoints,
                couponId,
                null, // 주문 생성 시점에는 결제 수단 미정
                OrderStatus.PENDING,
                now,
                now,
                expiresAt
        );
    }

    private static void validateOrderCreation(String orderNumber, Long userId, BigDecimal totalAmount,
                                             BigDecimal discountAmount, BigDecimal usedPoints) {
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
        BigDecimal validUsedPoints = (usedPoints != null) ? usedPoints : BigDecimal.ZERO;

        if (validDiscountAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw OrderException.orderCreationFailed("할인 금액은 0원 이상이어야 합니다. : " + discountAmount);
        }

        if (validUsedPoints.compareTo(BigDecimal.ZERO) < 0) {
            throw OrderException.orderCreationFailed("사용 포인트는 0원 이상이어야 합니다. : " + usedPoints);
        }
    }

    private static BigDecimal calculateFinalAmount(BigDecimal totalAmount, BigDecimal discountAmount, BigDecimal usedPoints) {
        BigDecimal validUsedPoints = (usedPoints != null) ? usedPoints : BigDecimal.ZERO;
        BigDecimal finalAmount = totalAmount.subtract(discountAmount).subtract(validUsedPoints);

        if (finalAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw OrderException.orderCreationFailed("최종 결제 금액이 0원 미만입니다.");
        }

        return finalAmount;
    }

    // orderNumber 생성
    public static String generateOrderNumber() {
        return ORDER_NUMBER_PREFIX + sequence.get() + "-" + LocalDateTime.now();
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
                this.usedPoints,
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
                this.usedPoints,
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
