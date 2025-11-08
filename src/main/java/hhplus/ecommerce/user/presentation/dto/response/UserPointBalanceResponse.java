package hhplus.ecommerce.user.presentation.dto.response;

import hhplus.ecommerce.user.domain.model.User;

import java.math.BigDecimal;

public record UserPointBalanceResponse (
    BigDecimal pointBalance
) {
    public static UserPointBalanceResponse of(User user) {
        return new UserPointBalanceResponse(user.getPointBalance());
    }
}

