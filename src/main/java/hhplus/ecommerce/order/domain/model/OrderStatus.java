package hhplus.ecommerce.order.domain.model;

public enum OrderStatus {
    PENDING("결제 대기"),
    PAID("결제 완료"),
    CANCELLED("주문 취소");

    public final String description;

    OrderStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
