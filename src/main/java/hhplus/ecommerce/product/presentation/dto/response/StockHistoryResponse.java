package hhplus.ecommerce.product.presentation.dto.response;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;

@Getter
@RequiredArgsConstructor
public class StockHistoryResponse {
    private final Long stockHistoryId;
    private final Long productOptionId;
    private final int amount;
    private final int balance;
    private final String description;
    private final Long updatedBy;
    private final LocalDateTime createdAt;
}
