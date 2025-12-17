package hhplus.ecommerce.order.application.service;

import hhplus.ecommerce.product.application.service.StockService;
import hhplus.ecommerce.product.domain.model.StockReservation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 주문 보상 서비스
 *
 * 외부 시스템 연동 실패 시 보상 트랜잭션을 처리
 * - 주문 취소
 * - 재고 예약 해제 (물리 재고 복구)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderCompensationService {

    private final OrderService orderService;
    private final StockService stockService;

    /**
     * 주문 보상 트랜잭션 실행
     *
     * 1. 주문 상태를 CANCELLED로 변경
     * 2. 예약된 재고 해제 (물리 재고 복구)
     *
     * @param orderId 취소할 주문 ID
     */
    @Transactional
    public void compensateOrder(Long orderId) {
        log.info("보상 트랜잭션 시작 - OrderId: {}", orderId);

        try {
            // 1. 주문 취소
            orderService.cancelOrder(orderId);
            log.info("주문 취소 완료 - OrderId: {}", orderId);

            // 2. 재고 예약 해제 (역순으로 롤백)
            List<StockReservation> reservations = stockService.getReservationsByOrderId(orderId);
            for (StockReservation reservation : reservations) {
                stockService.releaseStockReservation(reservation.getStockReservationId());
                log.info("재고 예약 해제 완료 - ReservationId: {}, ProductOptionId: {}, Quantity: {}",
                        reservation.getStockReservationId(),
                        reservation.getProductOptionId(),
                        reservation.getReservedQuantity());
            }

            log.info("보상 트랜잭션 성공 - OrderId: {}", orderId);

        } catch (Exception e) {
            log.error("보상 트랜잭션 실패 - OrderId: {}", orderId, e);
            throw e;
        }
    }
}
