package hhplus.ecommerce.order.domain.model;

public enum OrderItemStatus {
    PREPARING("상품 준비 중"),
    SHIPPING("배송 중"),
    DELIVERED("배송 완료"),
    CANCELLED("개별 상품 취소");

    public final String description;

    OrderItemStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
