package hhplus.ecommerce.product.application.service;

import hhplus.ecommerce.product.domain.model.PopularProduct;
import hhplus.ecommerce.product.domain.model.Product;
import hhplus.ecommerce.product.domain.model.ProductOption;
import hhplus.ecommerce.product.presentation.dto.response.ProductDetailResponse;
import hhplus.ecommerce.product.presentation.dto.response.ProductListResponse;
import hhplus.ecommerce.product.presentation.dto.response.ProductOptionResponse;
import hhplus.ecommerce.product.presentation.dto.response.TopProductResponse;
import org.springframework.stereotype.Component;
;
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
                popularProduct.getCalculationDate(),
                popularProduct.getRank(),
                popularProduct.getCreatedAt()
        );
    }
}
