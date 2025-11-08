package hhplus.ecommerce.point.presentation.dto.response;

import hhplus.ecommerce.point.domain.model.TransactionType;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@RequiredArgsConstructor
public class PointTransactionResponse {
    private final Long transactionId;
    private final Long userId;
    private final TransactionType transactionType;
    private final BigDecimal amount;
    private final BigDecimal balanceAfter;
    private final String description;
    private final LocalDateTime createdDate;
}