package hhplus.ecommerce.common.domain.exception;

import hhplus.ecommerce.common.domain.constants.ErrorCode;

import java.time.LocalDateTime;

public class StockException extends BusinessException {

    private StockException(String errorCode, String message) {
        super(errorCode, message);
    }

    private StockException(String errorCode, String message, Throwable cause) {
        super(errorCode, message, cause);
    }

    public static StockException stockNotFound(Long productOptionId) {
        String message = String.format("재고 정보를 찾을 수 없습니다. [옵션ID: %d]", productOptionId);
        return new StockException(ErrorCode.STOCK_NOT_FOUND, message);
    }

    public static StockException stockQuantityInsufficient(Long productOptionId, int requested, int available) {
        String message = String.format("재고가 부족합니다. [옵션ID: %d, 요청수량: %d, 현재수량: %d]",
                productOptionId, requested, available);
        return new StockException(ErrorCode.STOCK_QUANTITY_INSUFFICIENT, message);
    }

    public static StockException invalidStockAmount(int amount) {
        String message = String.format("유효하지 않은 재고 수량입니다. [요청수량: %d]", amount);
        return new StockException(ErrorCode.INVALID_STOCK_AMOUNT, message);
    }

    public static StockException stockUpdateUnauthorized(Long userId) {
        String message = String.format("재고 수정 권한이 없습니다. [사용자ID: %d]", userId);
        return new StockException(ErrorCode.STOCK_UPDATE_UNAUTHORIZED, message);
    }

    public static StockException stockConcurrencyConflict(Long productOptionId) {
        String message = String.format("재고 처리 중 동시성 충돌이 발생했습니다. [옵션ID: %d]", productOptionId);
        return new StockException(ErrorCode.STOCK_CONCURRENCY_CONFLICT, message);
    }

    public static StockException stockReservationNotFound(Long reservationId) {
        String message = String.format("재고 예약 정보를 찾을 수 없습니다. [예약ID: %d]", reservationId);
        return new StockException(ErrorCode.STOCK_RESERVATION_FAILED, message);
    }

    public static StockException stockReservationExpired(Long reservationId) {
        String message = String.format("재고 예약이 만료되었습니다. [예약ID: %d]", reservationId);
        return new StockException(ErrorCode.STOCK_RESERVATION_FAILED, message);
    }

    public static StockException stockReservationAlreadyConfirmed(Long reservationId) {
        String message = String.format("이미 확정된 재고 예약입니다. [예약ID: %d]", reservationId);
        return new StockException(ErrorCode.STOCK_RESERVATION_FAILED, message);
    }

    public static StockException stockReservationAlreadyReleased(Long reservationId) {
        String message = String.format("이미 해제된 재고 예약입니다. [예약ID: %d]", reservationId);
        return new StockException(ErrorCode.STOCK_RESERVATION_FAILED, message);
    }

    public static StockException stockReservationInvalidParameters(Long reservationId, Long productOptionId, Long orderId) {
        String message = String.format("유효하지 않은 재고 예약 파라미터입니다. [예약ID: %d, 상품옵션ID: %d, 주문ID: %d, 만료날짜: %s]", reservationId, productOptionId, orderId);
        return new StockException(ErrorCode.STOCK_RESERVATION_FAILED, message);
    }

    public static StockException stockReservationInvalidParameters(Long reservationId, Long productOptionId, Long orderId, LocalDateTime expiredDate) {
        String message = String.format("유효하지 않은 재고 예약 파라미터입니다. [예약ID: %d, 상품옵션ID: %d, 주문ID: %d, 만료날짜: %s]", reservationId, productOptionId, orderId, expiredDate.toString());
        return new StockException(ErrorCode.STOCK_RESERVATION_FAILED, message);
    }

    public static StockException stockDataInconsistency(int physicalQuantity, int reservedQuantity) {
        String message = String.format("예약된 재고량이 실제 재고량보다 많습니다. [실제 재고량 : %d, 예약 재고량 : %d]", physicalQuantity, reservedQuantity);
        return new StockException(ErrorCode.STOCK_RESERVATION_FAILED, message);
    }
}
