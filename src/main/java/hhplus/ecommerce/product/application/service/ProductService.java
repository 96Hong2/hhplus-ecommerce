package hhplus.ecommerce.product.application.service;

import hhplus.ecommerce.common.domain.constants.BusinessConstants;
import hhplus.ecommerce.common.domain.exception.ProductException;
import hhplus.ecommerce.common.presentation.response.PageResponse;
import hhplus.ecommerce.product.domain.model.Product;
import hhplus.ecommerce.product.domain.model.ProductOption;
import hhplus.ecommerce.product.domain.repository.ProductOptionRepository;
import hhplus.ecommerce.product.domain.repository.ProductRepository;
import hhplus.ecommerce.product.presentation.dto.response.ProductDetailResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductOptionRepository productOptionRepository;

    /**
     * 상품 등록
     * @param productName 상품명
     * @param category 카테고리
     * @param description 상품 설명
     * @param imageUrl 이미지 URL
     * @param price 상품 기본 가격
     * @param isExposed 노출 여부
     * @return 등록된 상품
     */
    public Product registerProduct(String productName, String category, String description,
                                   String imageUrl, BigDecimal price, boolean isExposed) {

        try {
            Product product = Product.create(productName, category, description, imageUrl, price, isExposed);
            return productRepository.save(product);
        } catch (Exception e) {
            throw ProductException.creationFailed(e.getMessage());
        }
    }

    /**
     * 상품 목록 조회 (페이징)
     * @param page 페이지 번호
     * @param size 페이지 크기
     * @param sort 정렬 기준 (latest, sales, price_asc, price_desc)
     * @param category 카테고리 필터 (선택)
     * @return 상품 목록 및 페이징 정보
     */
    public PageResponse<Product> getProducts(int page, int size, String sort, String category) {
        return switch (sort) {
            case "latest" -> productRepository.findAllByCategoryWithPage(page, size, category);
            case "sales" -> productRepository.findAllBySalesWithPaging(page, size);
            case "price_asc" -> productRepository.findAllByPriceWithPaging(page, size, true);
            case "price_desc" -> productRepository.findAllByPriceWithPaging(page, size, false);
            default -> throw ProductException.getListFailed("지원하지 않는 정렬 기준입니다: " + sort);
        };
    }

    /**
     * 상품 상세 조회 (옵션 포함)
     * @param productId 상품 ID
     * @return 상품 상세 정보
     */
    public ProductDetailResponse getProductDetail(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> ProductException.productNotFound(productId, ""));

        List<ProductOption> productOptionList = productOptionRepository.findAllByProductId(productId);

        return new ProductDetailResponse(
                productId,
                product.getProductName(),
                product.getCategory(),
                product.getDescription(),
                product.getImageUrl(),
                product.getPrice(),
                product.isExposed(),
                product.getCreatedAt(),
                product.getUpdatedAt(),
                productOptionList
        );
    }

    /**
     * 상품 수정
     * @param productId 상품 ID
     * @param productName 상품명
     * @param category 카테고리
     * @param description 상품 설명
     * @param imageUrl 이미지 URL
     * @param price 상품 기본 가격
     * @param isExposed 노출 여부
     * @return 수정된 상품
     */
    public Product updateProduct(Long productId, String productName, String category,
                                String description, String imageUrl, BigDecimal price, long salesCount, boolean isExposed) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> ProductException.productNotFound(productId, productName));

        product.update(productName, category, description, imageUrl, price, salesCount, isExposed);

        return productRepository.save(product);
    }

    /**
     * 상품 삭제 (논리 삭제)
     * @param productId 상품 ID
     */
    public void deleteProduct(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> ProductException.productNotFound(productId, ""));

        product.delete();

        productRepository.save(product);
    }

    /**
     * 상품 옵션 리스트 조회
     * @param productId 상품 ID
     * @return 상품 옵션 목록
     */
    public List<ProductOption> getProductOptions(Long productId) {
        productRepository.findById(productId)
                .orElseThrow(() -> ProductException.productNotFound(productId, ""));

        return productOptionRepository.findAllByProductId(productId);
    }

    /**
     * 인기 상품 조회 (최근 집계일수간 판매량 기준)
     * @param TopN 조회할 상품 개수
     * @param searchDays 집계 일수
     * @return 인기 상품 목록
     */
    public List<Product> getTopProducts(int TopN, int searchDays) {
        if (TopN > BusinessConstants.MAX_RANK || TopN <= 0 || searchDays <= 0) {
            String message = String.format("집계 일수는 1 이상, 조회할 상품 수는 1 이상 %d개 이하여야 합니다. 조회할 상품 수: %d, 집계 일수: %d", BusinessConstants.MAX_RANK, TopN, searchDays);
            throw ProductException.getListFailed(message);
        }

        return productRepository.findTopN(TopN, searchDays);
    }

    /**
     * 상품 옵션 등록
     * @param productId 상품 ID
     * @param optionName 옵션명
     * @param priceAdjustment 옵션 가격 조정값 (추가/감소)
     * @param stockQuantity 재고 수량
     * @param isExposed 노출 여부
     * @return 등록된 상품 옵션
     */
    public ProductOption createProductOption(Long productId, String optionName,
                                     BigDecimal priceAdjustment, int stockQuantity, boolean isExposed) {
        // 상품 존재 여부 검증
        productRepository.findById(productId)
                .orElseThrow(() -> ProductException.productNotFound(productId, ""));

        try {
            ProductOption productOption = ProductOption.create(productId, optionName, priceAdjustment, stockQuantity, isExposed);
            return productOptionRepository.save(productOption);
        } catch (Exception e) {
            throw ProductException.creationFailed(e.getMessage());
        }
    }

    /**
     * 상품 옵션 수정
     * @param productOptionId 상품 옵션 ID
     * @param optionName 옵션명
     * @param priceAdjustment 옵션 가격 조정값
     * @param isExposed 노출 여부
     * @return 수정된 상품 옵션
     */
    public ProductOption updateProductOption(Long productOptionId, String optionName,
                                     BigDecimal priceAdjustment, boolean isExposed) {
        ProductOption productOption = productOptionRepository.findById(productOptionId)
                .orElseThrow(() -> ProductException.productOptionNotFound(productOptionId));

        productOption.update(optionName, priceAdjustment, isExposed);

        return productOptionRepository.save(productOption);
    }

    /**
     * 상품 옵션 삭제
     * @param productOptionId 상품 옵션 ID
     */
    public void deleteProductOption(Long productOptionId) {
        ProductOption productOption = productOptionRepository.findById(productOptionId)
                .orElseThrow(() -> ProductException.productOptionNotFound(productOptionId));

        productOption.hide(); // isExposed 를 false로 처리
        productOptionRepository.save(productOption);
    }
}
