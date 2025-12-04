package hhplus.ecommerce.product.domain.model;

public enum PeriodType {
    DAILY("일간"),
    WEEKLY("주간"),
    MONTHLY("월간"),
    ALL_TIME("전체기간");

    private final String description;

    PeriodType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
