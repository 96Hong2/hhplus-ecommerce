package hhplus.ecommerce.order.domain.model;

/**
 * 결제 수단
 */
public enum PaymentMethod {
    CREDIT,  // 신용카드
    CHECK,   // 체크카드
    CASH,    // 현금
    KAKAO,   // 카카오페이
    POINT    // 포인트 결제
}
