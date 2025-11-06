package hhplus.ecommerce.product.domain.model;

public enum ReservationStatus {
    RESERVED("예약중"),
    CONFIRMED("확정"),
    RELEASED("해제");

    private final String description;

    ReservationStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
