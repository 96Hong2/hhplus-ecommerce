package hhplus.ecommerce.product.presentation.dto.response;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@RequiredArgsConstructor
public class ProductListResponse {
    private final Long productId;
    private final String productName;
    private final String category;
    private final String description;
    private final String imageUrl;
    private final BigDecimal price;
    private final boolean isExposed;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;
}
