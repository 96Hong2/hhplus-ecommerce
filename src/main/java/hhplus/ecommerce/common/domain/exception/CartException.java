package hhplus.ecommerce.common.domain.exception;

import hhplus.ecommerce.common.domain.constants.ErrorCode;

public class CartException extends BusinessException {

    private CartException(String errorCode, String message) {
        super(errorCode, message);
    }

    private CartException(String errorCode, String message, Throwable cause) {
        super(errorCode, message, cause);
    }

    public static CartException cartItemNotFound(Long cartId) {
        String message = String.format("장바구니 항목을 찾을 수 없습니다. [장바구니ID: %d]", cartId);
        return new CartException(ErrorCode.CART_ITEM_NOT_FOUND, message);
    }

    public static CartException cartEmpty(Long userId) {
        String message = String.format("장바구니가 비어있습니다. [사용자ID: %d]", userId);
        return new CartException(ErrorCode.CART_EMPTY, message);
    }

    public static CartException invalidCartQuantity(int quantity) {
        String message = String.format("유효하지 않은 수량입니다. [수량: %d]", quantity);
        return new CartException(ErrorCode.INVALID_CART_QUANTITY, message);
    }

    public static CartException cartAddFailed(String reason) {
        String message = String.format("장바구니 추가에 실패했습니다. [사유: %s]", reason);
        return new CartException(ErrorCode.CART_ADD_FAILED, message);
    }

    public static CartException cartUpdateFailed(Long cartId, String reason) {
        String message = String.format("장바구니 수정에 실패했습니다. [장바구니ID: %d, 사유: %s]", cartId, reason);
        return new CartException(ErrorCode.CART_UPDATE_FAILED, message);
    }

    public static CartException cartItemAlreadyExists(Long userId, Long productOptionId) {
        String message = String.format("이미 장바구니에 존재하는 상품입니다. [사용자ID: %d, 옵션ID: %d]",
                userId, productOptionId);
        return new CartException(ErrorCode.CART_ITEM_ALREADY_EXISTS, message);
    }
}
