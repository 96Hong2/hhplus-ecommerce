package hhplus.ecommerce.product.infrastructure.repository;

import hhplus.ecommerce.product.domain.model.ReservationStatus;
import hhplus.ecommerce.product.domain.model.StockReservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface StockReservationJpaRepository extends JpaRepository<StockReservation, Long> {

    // 주문별 재고 예약 조회
    List<StockReservation> findByOrderId(Long orderId);

    // 상품 옵션별 재고 예약 조회
    @Query("SELECT sr FROM StockReservation sr WHERE sr.productOptionId = :productOptionId AND sr.reservationStatus = :status")
    List<StockReservation> findByProductOptionIdAndStatus(@Param("productOptionId") Long productOptionId,
                                                           @Param("status") ReservationStatus status);

    // 만료된 재고 예약 조회
    @Query("SELECT sr FROM StockReservation sr WHERE sr.reservationStatus = 'RESERVED' AND sr.expiresAt < :currentTime")
    List<StockReservation> findExpiredReservations(@Param("currentTime") LocalDateTime currentTime);

    // 주문별 + 상태별 재고 예약 조회
    @Query("SELECT sr FROM StockReservation sr WHERE sr.orderId = :orderId AND sr.reservationStatus = :status")
    List<StockReservation> findByOrderIdAndStatus(@Param("orderId") Long orderId, @Param("status") ReservationStatus status);

    // 상품 옵션의 예약된 총 수량 계산
    @Query("SELECT COALESCE(SUM(sr.reservedQuantity), 0) FROM StockReservation sr WHERE sr.productOptionId = :productOptionId AND sr.reservationStatus = 'RESERVED'")
    int sumReservedQuantityByProductOptionId(@Param("productOptionId") Long productOptionId);
}
