package hhplus.ecommerce.product.application.dto;

import lombok.Getter;

/**
 * 인기 상품 랭킹 DTO
 */
@Getter
public class ProductRankingDto {
    private final Long productId;
    private final String productName;
    private final String imageUrl;
    private final int salesCount;  // 판매량
    private final int rank;        // 순위 (1위, 2위, ...)

    public ProductRankingDto(Long productId, String productName, String imageUrl, int salesCount, int rank) {
        this.productId = productId;
        this.productName = productName;
        this.imageUrl = imageUrl;
        this.salesCount = salesCount;
        this.rank = rank;
    }
}
