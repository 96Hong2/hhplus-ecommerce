package hhplus.ecommerce.coupon.domain.model;

public enum DiscountType {
    PERCENTAGE("정률"),
    FIXED("정액");

    public final String description;

    DiscountType(String description) {
        this.description = description;
    }
}
