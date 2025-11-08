package hhplus.ecommerce.product.domain.repository;

import hhplus.ecommerce.product.domain.model.ReservationStatus;
import hhplus.ecommerce.product.domain.model.StockReservation;

import java.util.List;
import java.util.Optional;

public interface StockReservationRepository {

    /**
     * 재고 예약 등록
     * @param stockReservation
     * @return 재고 예약
     */
    StockReservation save(StockReservation stockReservation);

    /**
     * 재고 예약Id로  조회
     * @param stockReservationId
     * @return 재고 예약
     */
    Optional<StockReservation> findByStockReservationId(Long stockReservationId);

    /**
     * 상품옵션 ID 및 주문ID로 재고 예약 조회
     * @param productOptionId
     * @param orderId
     * @return 재고 예약
     */
    Optional<StockReservation> findByProductOptionIdAndOrderId(Long productOptionId, Long orderId);

    /**
     * 상품옵션에 걸려있는 재고 예약 목록 조회
     * @param productOptionId
     * @return 재고 예약 목록
     */
    List<StockReservation> findAllByProductOptionId(Long productOptionId);

    /**
     * 상품옵션에 걸려있는 예약중 상태의 재고 예약목록 조회
     * @param productOptionId
     * @return
     */
    List<StockReservation> findAllReservedByProductOptionId(Long productOptionId);

    /**
     * 예약상태에 따른 재고 예약 목록 조회
     * @param reservationStatus
     * @return 재고 예약 목록
     */
    List<StockReservation> findAllByReservationStatus(ReservationStatus reservationStatus);
}
