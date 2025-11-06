package hhplus.ecommerce.product.presentation.dto.response;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@RequiredArgsConstructor
public class ProductOptionResponse {
    private final Long productOptionId;
    private final Long productId;
    private final String optionName;
    private final BigDecimal priceAdjustment;
    private final int stockQuantity;
    private final boolean isExposed;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;
}
