package hhplus.ecommerce.domain.model.user;

public enum UserRole {
    CUSTOMER("고객"),
    ADMIN("관리자");

    private final String description;

    UserRole(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
