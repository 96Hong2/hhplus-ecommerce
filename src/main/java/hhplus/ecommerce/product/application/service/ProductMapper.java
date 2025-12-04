package hhplus.ecommerce.product.application.service;

import hhplus.ecommerce.product.application.dto.ProductRankingDto;
import hhplus.ecommerce.product.domain.model.PopularProduct;
import hhplus.ecommerce.product.domain.model.Product;
import hhplus.ecommerce.product.domain.model.ProductOption;
import hhplus.ecommerce.product.presentation.dto.response.ProductDetailResponse;
import hhplus.ecommerce.product.presentation.dto.response.ProductListResponse;
import hhplus.ecommerce.product.presentation.dto.response.ProductOptionResponse;
import hhplus.ecommerce.product.presentation.dto.response.TopProductResponse;
import org.springframework.stereotype.Component;
import java.util.List;

@Component
public class ProductMapper {
    public ProductListResponse toProductListResponse(Product product) {
        return new ProductListResponse(
                product.getProductId(),
                product.getProductName(),
                product.getCategory(),
                product.getDescription(),
                product.getImageUrl(),
                product.getPrice(),
                product.isExposed(),
                product.getCreatedAt(),
                product.getUpdatedAt()
        );
    }

    public ProductOptionResponse toProductOptionResponse(ProductOption productOption) {
        return new ProductOptionResponse(
                productOption.getProductOptionId(),
                productOption.getProductId(),
                productOption.getOptionName(),
                productOption.getPriceAdjustment(),
                productOption.getStockQuantity(),
                productOption.isExposed(),
                productOption.isSoldOut(),
                productOption.getCreatedAt(),
                productOption.getUpdatedAt()
        );
    }

    public ProductDetailResponse toProductDetailResponse(Product product, List<ProductOption> productOptionList) {
        List<ProductOptionResponse> optionResponses = productOptionList.stream()
                .map(this::toProductOptionResponse)
                .toList();
        return new ProductDetailResponse(
                product.getProductId(),
                product.getProductName(),
                product.getCategory(),
                product.getDescription(),
                product.getImageUrl(),
                product.getPrice(),
                product.isExposed(),
                product.getCreatedAt(),
                product.getUpdatedAt(),
                optionResponses
        );
    }

    public TopProductResponse toTopProductResponse(PopularProduct popularProduct) {
        return new TopProductResponse(
                popularProduct.getPopularProductId(),
                popularProduct.getProductId(),
                popularProduct.getSalesCount(),
                popularProduct.getCalculationDate().atStartOfDay(),
                popularProduct.getRank(),
                popularProduct.getCreatedAt()
        );
    }

    /**
     * ProductRankingDto를 ProductListResponse로 변환 (인기 상품 조회용)
     */
    public ProductListResponse toProductListResponse(ProductRankingDto rankingDto) {
        return new ProductListResponse(
                rankingDto.getProductId(),
                rankingDto.getProductName(),
                null, // category - 랭킹 DTO에는 없음
                null, // description - 랭킹 DTO에는 없음
                rankingDto.getImageUrl(),
                null, // price - 랭킹 DTO에는 없음
                true, // isExposed - 랭킹에 노출된 상품은 당연히 exposed
                null, // createdAt - 랭킹 DTO에는 없음
                null  // updatedAt - 랭킹 DTO에는 없음
        );
    }
}
