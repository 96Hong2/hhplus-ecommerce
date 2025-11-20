package hhplus.ecommerce.common.domain.exception;

import hhplus.ecommerce.common.domain.constants.ErrorCode;

import java.math.BigDecimal;

public class OrderException extends BusinessException {

    private OrderException(String errorCode, String message) {
        super(errorCode, message);
    }

    private OrderException(String errorCode, String message, Throwable cause) {
        super(errorCode, message, cause);
    }

    public static OrderException orderNotFound(Long orderId) {
        String message = String.format("주문을 찾을 수 없습니다. [주문ID: %d]", orderId);
        return new OrderException(ErrorCode.ORDER_NOT_FOUND, message);
    }

    public static OrderException orderNotFoundByNumber(String orderNumber) {
        String message = String.format("주문을 찾을 수 없습니다. [주문번호: %s]", orderNumber);
        return new OrderException(ErrorCode.ORDER_NOT_FOUND, message);
    }

    public static OrderException orderItemNotFound(Long orderItemId) {
        String message = String.format("주문 상품을 찾을 수 없습니다. [주문상품ID: %d]", orderItemId);
        return new OrderException(ErrorCode.ORDER_ITEM_NOT_FOUND, message);
    }

    public static OrderException orderCreationFailed(String reason) {
        String message = String.format("주문 생성에 실패했습니다. [사유: %s]", reason);
        return new OrderException(ErrorCode.ORDER_CREATION_FAILED, message);
    }

    public static OrderException invalidOrderStatus(String currentStatus, String requestedStatus) {
        String message = String.format("유효하지 않은 주문 상태 변경입니다. [현재상태: %s, 요청상태: %s]",
                currentStatus, requestedStatus);
        return new OrderException(ErrorCode.INVALID_ORDER_STATUS, message);
    }

    public static OrderException orderCancelNotAllowed(Long orderId, String status) {
        String message = String.format("주문 취소가 불가능한 상태입니다. [주문ID: %d, 상태: %s]", orderId, status);
        return new OrderException(ErrorCode.ORDER_CANCEL_NOT_ALLOWED, message);
    }

    public static OrderException paymentFailed(String reason) {
        String message = String.format("결제에 실패했습니다. [사유: %s]", reason);
        return new OrderException(ErrorCode.PAYMENT_FAILED, message);
    }

    public static OrderException paymentAmountMismatch(BigDecimal expected, BigDecimal actual) {
        String message = String.format("결제 금액이 일치하지 않습니다. [예상금액: %f, 실제금액: %f]",
                expected, actual);
        return new OrderException(ErrorCode.PAYMENT_AMOUNT_MISMATCH, message);
    }

    public static OrderException invalidPaymentMethod(String method) {
        String message = String.format("유효하지 않은 결제 수단입니다. [결제수단: %s]", method);
        return new OrderException(ErrorCode.INVALID_PAYMENT_METHOD, message);
    }

    public static OrderException orderItemsEmpty() {
        String message = "주문 상품이 비어있습니다.";
        return new OrderException(ErrorCode.ORDER_ITEMS_EMPTY, message);
    }

    public static OrderException orderStatusUpdateFailed(Long orderId, String reason) {
        String message = String.format("주문 상태 업데이트에 실패했습니다. [주문ID: %d, 사유: %s]", orderId, reason);
        return new OrderException(ErrorCode.ORDER_STATUS_UPDATE_FAILED, message);
    }

    public static OrderException orderTimeout(Long orderId) {
        String message = String.format("주문 시간이 초과되었습니다. [주문ID: %d]", orderId);
        return new OrderException(ErrorCode.ORDER_TIMEOUT, message);
    }

    public static OrderException orderAlreadyPaid(Long orderId) {
        String message = String.format("이미 결제 완료된 주문입니다. [주문ID: %d]", orderId);
        return new OrderException(ErrorCode.ORDER_ALREADY_PAID, message);
    }

    public static OrderException orderAlreadyCancelled(Long orderId) {
        String message = String.format("이미 취소된 주문입니다. [주문ID: %d]", orderId);
        return new OrderException(ErrorCode.ORDER_ALREADY_CANCELLED, message);
    }

    public static OrderException invalidOrderItemStatus(String currentStatus, String requestedStatus) {
        String message = String.format("유효하지 않은 주문 상품 상태 변경입니다. [현재상태: %s, 요청상태: %s]",
                currentStatus, requestedStatus);
        return new OrderException(ErrorCode.INVALID_ORDER_ITEM_STATUS, message);
    }
}
