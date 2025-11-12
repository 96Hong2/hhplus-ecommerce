package hhplus.ecommerce.product.presentation.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@RequiredArgsConstructor
public class ProductDetailResponse {
    private final Long productId;
    private final String productName;
    private final String category;
    private final String description;
    private final String imageUrl;
    private final BigDecimal price;
    @JsonProperty("is_exposed")
    private final boolean isExposed;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    private final List<ProductOptionResponse> productOptions;
}
