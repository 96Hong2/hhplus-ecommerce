package hhplus.ecommerce.user.domain.model;

import lombok.Getter;

@Getter
public enum UserRole {
    CUSTOMER("고객"),
    ADMIN("관리자");

    private final String description;

    UserRole(String description) {
        this.description = description;
    }

}
