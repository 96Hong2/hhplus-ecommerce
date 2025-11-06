package hhplus.ecommerce.product.presentation.dto.response;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;

@Getter
@RequiredArgsConstructor
public class TopProductResponse {
    private final Long popularProductId;
    private final Long productId;
    private final int salesCount;
    private final LocalDateTime calculationDate;
    private final int rank;
    private final LocalDateTime createdAt;
}
