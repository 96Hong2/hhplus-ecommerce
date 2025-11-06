package hhplus.ecommerce.common.domain.exception;

import hhplus.ecommerce.common.domain.constants.ErrorCode;

import java.math.BigDecimal;

public class CouponException extends BusinessException {

    private CouponException(String errorCode, String message) {
        super(errorCode, message);
    }

    private CouponException(String errorCode, String message, Throwable cause) {
        super(errorCode, message, cause);
    }

    public static CouponException couponNotFound(Long couponId) {
        String message = String.format("쿠폰을 찾을 수 없습니다. [쿠폰ID: %d]", couponId);
        return new CouponException(ErrorCode.COUPON_NOT_FOUND, message);
    }

    public static CouponException couponExpired(Long couponId) {
        String message = String.format("만료된 쿠폰입니다. [쿠폰ID: %d]", couponId);
        return new CouponException(ErrorCode.COUPON_EXPIRED, message);
    }

    public static CouponException couponAlreadyUsed(Long userCouponId) {
        String message = String.format("이미 사용된 쿠폰입니다. [사용자쿠폰ID: %d]", userCouponId);
        return new CouponException(ErrorCode.COUPON_ALREADY_USED, message);
    }

    public static CouponException couponMinOrderNotMet(BigDecimal minAmount, BigDecimal orderAmount) {
        String message = String.format("최소 주문 금액을 충족하지 않습니다. [최소금액: %f, 주문금액: %f]",
                minAmount, orderAmount);
        return new CouponException(ErrorCode.COUPON_MIN_ORDER_NOT_MET, message);
    }

    public static CouponException couponIssueLimitExceeded(Long couponId) {
        String message = String.format("쿠폰 발급 수량이 초과되었습니다. [쿠폰ID: %d]", couponId);
        return new CouponException(ErrorCode.COUPON_ISSUE_LIMIT_EXCEEDED, message);
    }

    public static CouponException userCouponNotFound(Long userCouponId) {
        String message = String.format("사용자 쿠폰을 찾을 수 없습니다. [사용자쿠폰ID: %d]", userCouponId);
        return new CouponException(ErrorCode.USER_COUPON_NOT_FOUND, message);
    }

    public static CouponException couponAlreadyIssued(Long userId, Long couponId) {
        String message = String.format("이미 발급된 쿠폰입니다. [사용자ID: %d, 쿠폰ID: %d]", userId, couponId);
        return new CouponException(ErrorCode.COUPON_ALREADY_ISSUED, message);
    }

    public static CouponException invalidCouponDiscountType(String discountType) {
        String message = String.format("유효하지 않은 할인 타입입니다. [할인타입: %s]", discountType);
        return new CouponException(ErrorCode.INVALID_COUPON_DISCOUNT_TYPE, message);
    }

    public static CouponException couponIssueFailed(String reason) {
        String message = String.format("쿠폰 발급에 실패했습니다. [사유: %s]", reason);
        return new CouponException(ErrorCode.COUPON_ISSUE_FAILED, message);
    }

    public static CouponException couponIssueRaceFailed(Long couponId) {
        String message = String.format("쿠폰 선착순 발급 경쟁에 실패했습니다. [쿠폰ID: %d]", couponId);
        return new CouponException(ErrorCode.COUPON_ISSUE_RACE_FAILED, message);
    }

    public static CouponException couponNotValidYet(Long couponId) {
        String message = String.format("아직 사용할 수 없는 쿠폰입니다. [쿠폰ID: %d]", couponId);
        return new CouponException(ErrorCode.COUPON_NOT_VALID_YET, message);
    }

    public static CouponException couponCreateFail(String reason) {
        String message = String.format("쿠폰 생성에 실패했습니다. [사유: %s]", reason);
        return new CouponException(ErrorCode.COUPON_CREATE_FAILED, message);
    }
}
