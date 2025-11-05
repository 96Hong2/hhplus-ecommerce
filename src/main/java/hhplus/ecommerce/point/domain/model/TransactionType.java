package hhplus.ecommerce.point.domain.model;

public enum TransactionType {
    CHARGE("충전"),
    USE("사용");

    private final String description;

    TransactionType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
