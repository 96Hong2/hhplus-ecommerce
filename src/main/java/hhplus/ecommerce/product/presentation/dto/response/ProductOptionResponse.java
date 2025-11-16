package hhplus.ecommerce.product.presentation.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
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
    @JsonProperty("is_exposed")
    private final boolean is_exposed;
    @JsonProperty("is_sold_out")
    private final boolean is_sold_out;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;
}
