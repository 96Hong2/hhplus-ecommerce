package hhplus.ecommerce.common.domain.constants;

public class ErrorCode {
    // ========== 상품 (P) ==========
    public static final String PRODUCT_NOT_FOUND = "P001";
    public static final String INSUFFICIENT_STOCK = "P002";
    public static final String PRODUCT_NOT_EXPOSED = "P003";
    public static final String PRODUCT_OPTION_NOT_FOUND = "P004";
    public static final String PRODUCT_OPTION_SOLD_OUT = "P005";
    public static final String INVALID_PRODUCT_CATEGORY = "P006";
    public static final String PRODUCT_CREATION_FAILED = "P007";

    // ========== 재고 (S) ==========
    public static final String STOCK_NOT_FOUND = "S001";
    public static final String STOCK_QUANTITY_INSUFFICIENT = "S002";
    public static final String INVALID_STOCK_AMOUNT = "S003";
    public static final String STOCK_UPDATE_UNAUTHORIZED = "S004";
    public static final String STOCK_CONCURRENCY_CONFLICT = "S005";
    public static final String STOCK_RESERVATION_NOT_FOUND = "S006";
    public static final String STOCK_RESERVATION_EXPIRED = "S007";
    public static final String STOCK_RESERVATION_ALREADY_CONFIRMED = "S008";
    public static final String STOCK_RESERVATION_ALREADY_RELEASED = "S009";

    // ========== 유저 (U) ==========
    public static final String USER_NOT_FOUND = "U001";
    public static final String USER_CREATION_FAILED = "U002";
    public static final String USER_GET_LIST_FAILED = "U003";
    public static final String USER_AUTHENTICATION_FAILED = "U004";
    public static final String USER_AUTHORIZATION_FAILED = "U005";

    // ========== 포인트 (PT) ==========
    public static final String INSUFFICIENT_POINT_BALANCE = "PT001";
    public static final String INVALID_POINT_AMOUNT = "PT002";
    public static final String POINT_HISTORY_NOT_FOUND = "PT003";
    public static final String POINT_CHARGE_FAILED = "PT004";
    public static final String POINT_USE_FAILED = "PT005";
    public static final String POINT_CONCURRENCY_ERROR = "PT006";

    // ========== 장바구니 (C) ==========
    public static final String CART_ITEM_NOT_FOUND = "C001";
    public static final String CART_EMPTY = "C002";
    public static final String INVALID_CART_QUANTITY = "C003";
    public static final String CART_ADD_FAILED = "C004";
    public static final String CART_UPDATE_FAILED = "C005";
    public static final String CART_ITEM_ALREADY_EXISTS = "C006";

    // ========== 주문 (O) ==========
    public static final String ORDER_NOT_FOUND = "O001";
    public static final String ORDER_ITEM_NOT_FOUND = "O002";
    public static final String ORDER_CREATION_FAILED = "O003";
    public static final String INVALID_ORDER_STATUS = "O004";
    public static final String ORDER_CANCEL_NOT_ALLOWED = "O005";
    public static final String PAYMENT_FAILED = "O006";
    public static final String PAYMENT_AMOUNT_MISMATCH = "O007";
    public static final String INVALID_PAYMENT_METHOD = "O008";
    public static final String ORDER_ITEMS_EMPTY = "O009";
    public static final String ORDER_STATUS_UPDATE_FAILED = "O010";
    public static final String ORDER_TIMEOUT = "O011";
    public static final String ORDER_ALREADY_PAID = "O012";
    public static final String ORDER_ALREADY_CANCELLED = "O013";
    public static final String INVALID_ORDER_ITEM_STATUS = "O014";

    // ========== 쿠폰 (CP) ==========
    public static final String COUPON_NOT_FOUND = "CP001";
    public static final String COUPON_EXPIRED = "CP002";
    public static final String COUPON_ALREADY_USED = "CP003";
    public static final String COUPON_MIN_ORDER_NOT_MET = "CP004";
    public static final String COUPON_ISSUE_LIMIT_EXCEEDED = "CP005";
    public static final String USER_COUPON_NOT_FOUND = "CP006";
    public static final String COUPON_ALREADY_ISSUED = "CP007";
    public static final String INVALID_COUPON_DISCOUNT_TYPE = "CP008";
    public static final String COUPON_ISSUE_FAILED = "CP009";
    public static final String COUPON_ISSUE_RACE_FAILED = "CP010";
    public static final String COUPON_NOT_VALID_YET = "CP011";

    // ========== 외부연동 (I) ==========
    public static final String INTEGRATION_FAILED = "I001";
    public static final String INTEGRATION_LOG_NOT_FOUND = "I002";
    public static final String LOGISTICS_INTEGRATION_FAILED = "I003";
    public static final String SALES_MANAGEMENT_INTEGRATION_FAILED = "I004";
    public static final String ERP_INTEGRATION_FAILED = "I005";
    public static final String INTEGRATION_RETRY_FAILED = "I006";
    public static final String INTEGRATION_MAX_RETRY_EXCEEDED = "I007";
    public static final String INVALID_INTEGRATION_TYPE = "I008";

    // ========== 공통 (E) ==========
    public static final String BAD_REQUEST = "E001";
    public static final String UNAUTHORIZED = "E002";
    public static final String FORBIDDEN = "E003";
    public static final String NOT_FOUND = "E004";
    public static final String INTERNAL_SERVER_ERROR = "E500";
    public static final String DATABASE_ERROR = "E501";
    public static final String EXTERNAL_API_ERROR = "E502";
    public static final String VALIDATION_FAILED = "E503";
    public static final String TIMEOUT_ERROR = "E504";
}
