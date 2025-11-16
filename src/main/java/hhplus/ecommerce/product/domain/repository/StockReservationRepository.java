package hhplus.ecommerce.product.domain.repository;

import hhplus.ecommerce.product.domain.model.ReservationStatus;
import hhplus.ecommerce.product.domain.model.StockReservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface StockReservationRepository extends JpaRepository<StockReservation, Long> {

    List<StockReservation> findByOrderId(Long orderId);

    Optional<StockReservation> findByProductOptionIdAndOrderId(Long productOptionId, Long orderId);

    List<StockReservation> findByProductOptionId(Long productOptionId);

    @Query("SELECT sr FROM StockReservation sr WHERE sr.productOptionId = :productOptionId AND sr.reservationStatus = :status")
    List<StockReservation> findByProductOptionIdAndStatus(@Param("productOptionId") Long productOptionId,
                                                           @Param("status") ReservationStatus status);

    @Query("SELECT sr FROM StockReservation sr WHERE sr.reservationStatus = 'RESERVED' AND sr.expiresAt < :currentTime")
    List<StockReservation> findExpiredReservations(@Param("currentTime") LocalDateTime currentTime);

    @Query("SELECT sr FROM StockReservation sr WHERE sr.orderId = :orderId AND sr.reservationStatus = :status")
    List<StockReservation> findByOrderIdAndStatus(@Param("orderId") Long orderId, @Param("status") ReservationStatus status);

    @Query("SELECT COALESCE(SUM(sr.reservedQuantity), 0) FROM StockReservation sr WHERE sr.productOptionId = :productOptionId AND sr.reservationStatus = 'RESERVED'")
    int sumReservedQuantityByProductOptionId(@Param("productOptionId") Long productOptionId);

    List<StockReservation> findByReservationStatus(ReservationStatus reservationStatus);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT sr FROM StockReservation sr WHERE sr.stockReservationId = :reservationId")
    Optional<StockReservation> findByIdWithLock(@Param("reservationId") Long reservationId);
}
