package hhplus.ecommerce.common.domain.exception;

import hhplus.ecommerce.common.domain.constants.ErrorCode;

import java.math.BigDecimal;

public class PointException extends BusinessException {

    private PointException(String errorCode, String message) {
        super(errorCode, message);
    }

    private PointException(String errorCode, String message, Throwable cause) {
        super(errorCode, message, cause);
    }

    public static PointException insufficientPointBalance(Long userId, BigDecimal amount, BigDecimal balance) {
        String message = String.format(
                "포인트 잔액이 부족합니다. [사용자ID: %d, 요청금액: %f, 현재잔액: %f]",
                userId, amount, balance
        );
        return new PointException(ErrorCode.INSUFFICIENT_POINT_BALANCE, message);
    }

    public static PointException invalidPointAmount(BigDecimal amount) {
        String message = String.format(
                "유효하지 않은 포인트 금액입니다. [요청금액: %f, 최소금액: 1000원]",
                amount
        );
        return new PointException(ErrorCode.INVALID_POINT_AMOUNT, message);
    }

    public static PointException pointHistoryNotFound(BigDecimal amount) {
        String message = String.format(
                "포인트 히스토리를 찾을 수 없습니다. [요청금액 : %f]",
                amount
        );
        return new PointException(ErrorCode.POINT_HISTORY_NOT_FOUND, message);
    }

    public static PointException chargeFailed(Long userId, String reason) {
        String message = String.format(
                "포인트 충전에 실패했습니다. [사용자ID: %d, 사유: %s]",
                userId, reason
        );
        return new PointException(ErrorCode.POINT_CHARGE_FAILED, message);
    }

    public static PointException useFailed(Long userId, String reason) {
        String message = String.format(
                "포인트 사용에 실패했습니다. [사용자ID: %d, 사유: %s]",
                userId, reason
        );
        return new PointException(ErrorCode.POINT_USE_FAILED, message);
    }

    public static PointException concurrencyError(Long userId) {
        String message = String.format(
                "포인트 처리 중 동시성 오류가 발생했습니다. [사용자ID: %d]",
                userId
        );
        return new PointException(ErrorCode.POINT_CONCURRENCY_ERROR, message);
    }
}
