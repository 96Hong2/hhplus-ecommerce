package hhplus.ecommerce.point.presentation.dto.response;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;

@Getter
@RequiredArgsConstructor
public class PointBalanceResponse {
    private final Long userId;
    private final BigDecimal balance;
}
