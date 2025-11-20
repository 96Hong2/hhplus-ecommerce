package hhplus.ecommerce.product.domain.model;

import hhplus.ecommerce.common.domain.constants.BusinessConstants;
import hhplus.ecommerce.common.domain.exception.StockException;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "stock_reservations", indexes = {
    @Index(name = "idx_order_id", columnList = "order_id"),
    @Index(name = "idx_product_option_status", columnList = "product_option_id, reservation_status"),
    @Index(name = "idx_expires_at", columnList = "expires_at")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StockReservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long stockReservationId;

    @Column(name = "product_option_id", nullable = false)
    private Long productOptionId;

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @Column(name = "reserved_quantity", nullable = false)
    private int reservedQuantity;

    @Enumerated(EnumType.STRING)
    @Column(name = "reservation_status", nullable = false, length = 20)
    private ReservationStatus reservationStatus;

    @Column(name = "reserved_at", nullable = false, updatable = false)
    private LocalDateTime reservedAt;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private StockReservation(Long stockReservationId, Long productOptionId, Long orderId, int reservedQuantity,
                            ReservationStatus reservationStatus, LocalDateTime reservedAt, LocalDateTime expiresAt) {
        this.stockReservationId = stockReservationId;
        this.productOptionId = productOptionId;
        this.orderId = orderId;
        this.reservedQuantity = reservedQuantity;
        this.reservationStatus = reservationStatus;
        this.reservedAt = reservedAt;
        this.expiresAt = expiresAt;
    }

    /**
     * 재고 예약을 생성한다.
     * @param productOptionId 상품 옵션 ID
     * @param orderId 주문 ID
     * @param reservedQuantity 예약 수량
     * @return 생성된 재고 예약
     */
    public static StockReservation create(Long productOptionId, Long orderId, int reservedQuantity) {
        return create(productOptionId, orderId, reservedQuantity, LocalDateTime.now().plusMinutes(BusinessConstants.STOCK_RESERVATION_TIMEOUT_MINUTES));
    }

    /**
     * 재고 예약을 생성한다. (만료 시간 지정)
     * @param productOptionId 상품 옵션 ID
     * @param orderId 주문 ID
     * @param reservedQuantity 예약 수량
     * @param expiresAt 만료 시각
     * @return 생성된 재고 예약
     */
    public static StockReservation create(Long productOptionId, Long orderId, int reservedQuantity, LocalDateTime expiresAt) {
        validateProductOptionId(productOptionId);
        validateOrderId(orderId);
        validateReservedQuantity(reservedQuantity);
        validateExpiresAt(expiresAt);

        LocalDateTime now = LocalDateTime.now();
        return new StockReservation(null, productOptionId, orderId, reservedQuantity,
                                   ReservationStatus.RESERVED, now, expiresAt);
    }

    private static void validateProductOptionId(Long productOptionId) {
        if (productOptionId == null) {
            throw StockException.stockReservationInvalidParameters(null, productOptionId, null);
        }
    }

    private static void validateOrderId(Long orderId) {
        if (orderId == null) {
            throw StockException.stockReservationInvalidParameters(null, null, orderId);
        }
    }

    private static void validateReservedQuantity(int reservedQuantity) {
        if (reservedQuantity <= 0) {
            throw StockException.invalidStockAmount(reservedQuantity);
        }
    }

    private static void validateExpiresAt(LocalDateTime expiresAt) {
        if (LocalDateTime.now().isAfter(expiresAt)) {
            throw StockException.stockReservationInvalidParameters(null, null, null, expiresAt);
        }
    }

    /**
     * 예약 상태를 변경한다.
     * @param newStatus 새로운 예약 상태
     * @return 상태가 변경된 새로운 StockReservation 객체
     */
    public StockReservation updateStatus(ReservationStatus newStatus) {
        if (newStatus == null) {
            throw new IllegalArgumentException("예약 상태는 필수입니다.");
        }
        return new StockReservation(this.stockReservationId, this.productOptionId, this.orderId,
                                   this.reservedQuantity, newStatus, this.reservedAt, this.expiresAt);
    }

    /**
     * 예약을 확정한다.
     * @return 확정된 새로운 StockReservation 객체
     */
    public StockReservation confirm() {
        if (isExpired()) {
            throw StockException.stockReservationExpired(stockReservationId);
        }
        return updateStatus(ReservationStatus.CONFIRMED);
    }

    /**
     * 예약을 해제한다.
     * @return 해제된 새로운 StockReservation 객체
     */
    public StockReservation release() {
        return updateStatus(ReservationStatus.RELEASED);
    }

    /**
     * 예약이 만료되었는지 확인한다.
     * @return 만료 여부
     */
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }

    /**
     * 예약이 활성 상태인지 확인한다.
     * @return 활성 상태 여부
     */
    public boolean isActive() {
        return reservationStatus == ReservationStatus.RESERVED && !isExpired();
    }

    /**
     * 예약이 확정되었는지 확인한다.
     * @return 확정 여부
     */
    public boolean isConfirmed() {
        return reservationStatus == ReservationStatus.CONFIRMED;
    }
}
