package hhplus.ecommerce.common.domain.exception;

import hhplus.ecommerce.common.domain.constants.ErrorCode;

public class ProductException extends BusinessException {

    private ProductException(String errorCode, String message) {
        super(errorCode, message);
    }

    private ProductException(String errorCode, String message, Throwable cause) {
        super(errorCode, message, cause);
    }

    public static ProductException creationFailed(String reason) {
        String message = String.format("상품 등록에 실패했습니다. [사유: %s]", reason);

        return new ProductException(ErrorCode.PRODUCT_CREATION_FAILED, message);
    }

    public static ProductException productNotFound(Long productId, String productName) {
        String message = String.format("상품이 존재하지 않습니다. [상품ID: %d, 상품명: %s]",
                productId, productName);

        return new ProductException(ErrorCode.PRODUCT_NOT_FOUND, message);
    }

    public static ProductException insufficientStock(String optionName, int stockQuantity) {
        String message = String.format("상품옵션 재고가 부족합니다. [상품명: %s, 남은 재고 수: %d]",
                optionName, stockQuantity);

        return new ProductException(ErrorCode.INSUFFICIENT_STOCK, message);
    }

    public static ProductException productNotExposed(Long productId) {
        String message = String.format("노출되지 않은 상품입니다. [상품ID: %d]", productId);
        return new ProductException(ErrorCode.PRODUCT_NOT_EXPOSED, message);
    }

    public static ProductException productOptionNotFound(Long productOptionId) {
        String message = String.format("상품 옵션을 찾을 수 없습니다. [옵션ID: %d]", productOptionId);
        return new ProductException(ErrorCode.PRODUCT_OPTION_NOT_FOUND, message);
    }

    public static ProductException productOptionSoldOut(String optionName) {
        String message = String.format("품절된 상품 옵션입니다. [옵션명: %s]", optionName);
        return new ProductException(ErrorCode.PRODUCT_OPTION_SOLD_OUT, message);
    }

    public static ProductException invalidProductCategory(String category) {
        String message = String.format("유효하지 않은 상품 카테고리입니다. [카테고리: %s]", category);
        return new ProductException(ErrorCode.INVALID_PRODUCT_CATEGORY, message);
    }

    public static ProductException getListFailed(String reason) {
        String message = String.format("상품 목록 조회에 실패했습니다. [사유: %s]", reason);

        return new ProductException(ErrorCode.PRODUCT_GET_LIST_FAILED, message);
    }
}
