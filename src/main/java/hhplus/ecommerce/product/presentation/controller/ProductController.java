package hhplus.ecommerce.product.presentation.controller;

import hhplus.ecommerce.common.domain.exception.ProductException;
import hhplus.ecommerce.common.presentation.response.PageResponse;
import hhplus.ecommerce.product.application.service.ProductMapper;
import hhplus.ecommerce.product.application.service.ProductService;
import hhplus.ecommerce.product.domain.model.Product;
import hhplus.ecommerce.product.presentation.dto.request.ProductRegistrationRequest;
import hhplus.ecommerce.product.presentation.dto.request.ProductUpdateRequest;
import hhplus.ecommerce.product.presentation.dto.request.ProductOptionRegisterRequest;
import hhplus.ecommerce.product.presentation.dto.request.ProductOptionUpdateRequest;
import hhplus.ecommerce.product.presentation.dto.response.ProductDetailResponse;
import hhplus.ecommerce.product.presentation.dto.response.ProductListResponse;
import hhplus.ecommerce.product.presentation.dto.response.ProductOptionResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/product")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;
    private final ProductMapper productMapper;
    /**
     * 상품 목록 조회
     * GET /api/product
     * @param page 페이지 번호 (기본값: 0)
     * @param size 페이지 크기 (기본값: 20, 최대: 100)
     * @param sort 정렬 기준 (latest, sales, price_asc, price_desc)
     * @param category 카테고리 필터
     * @return 페이징된 상품 목록
     */
    @GetMapping
    public PageResponse<ProductListResponse> getProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "latest") String sort,
            @RequestParam(required = false) String category) {
        // 페이지 크기 상한 제한 적용
        if (size > 100) size = 100;

        // 정렬 기준 파라미터 검증
        List<String> validSortOptions = List.of("latest", "sales", "price_asc", "price_desc");
        if (!validSortOptions.contains(sort)) {
            throw ProductException.getListFailed("정렬 기준 파라미터가 부적절합니다. sort : " + sort);
        }

        PageResponse<Product> productPage = productService.getProducts(page, size, sort, category);
        List<ProductListResponse> contents = productPage.getContent().stream()
                .map(productMapper::toProductListResponse)
                .toList();

        return new PageResponse<>(
                contents,
                productPage.getPage(),
                productPage.getSize(),
                productPage.getTotalElements(),
                productPage.getTotalPages());
    }

    /**
     * 상품 등록
     * POST /api/product
     * @param request 상품 등록 요청
     * @return 등록된 상품
     */
    @PostMapping
    public ProductListResponse createProduct(@Valid @RequestBody ProductRegistrationRequest request) {
        Product newProduct = productService.registerProduct(
                request.getProductName(),
                request.getCategory(),
                request.getDescription(),
                request.getImageUrl(),
                request.getPrice(),
                request.isExposed()
        );
        return productMapper.toProductListResponse(newProduct);
    }

    /**
     * 상품 상세 조회
     * GET /api/product/{productId}
     * @param productId 상품 ID
     * @return 상품 상세 정보 (옵션 포함)
     */
    @GetMapping("/{productId}")
    public ProductDetailResponse getProductDetail(@PathVariable Long productId) {
        return productService.getProductDetail(productId);
    }

    /**
     * 상품 수정
     * PUT /api/product/{productId}
     * @param productId 상품 ID
     * @param request 상품 수정 요청
     * @return 수정된 상품
     */
    @PutMapping("/{productId}")
    public ProductListResponse updateProduct(
            @PathVariable Long productId,
            @Valid @RequestBody ProductUpdateRequest request) {
        Product updatedProduct = productService.updateProduct(
                productId,
                request.getProductName(),
                request.getCategory(),
                request.getDescription(),
                request.getImageUrl(),
                request.getPrice(),
                request.getSalesCount(),
                request.isExposed()
        );
        return productMapper.toProductListResponse(updatedProduct);
    }

    /**
     * 상품 삭제 (논리 삭제)
     * DELETE /api/product/{productId}
     * @param productId 상품 ID
     */
    @DeleteMapping("/{productId}")
    public void deleteProduct(@PathVariable Long productId) {
        productService.deleteProduct(productId);
    }

    /**
     * 상품 옵션 리스트 조회
     * GET /api/product/options/{productId}
     * @param productId 상품 ID
     * @return 상품 옵션 목록
     */
    @GetMapping("/options/{productId}")
    public List<ProductOptionResponse> getProductOptions(@PathVariable Long productId) {
        return productService.getProductOptions(productId).stream()
                .map(productMapper::toProductOptionResponse)
                .collect(Collectors.toList());
    }

    /**
     * 인기 상품 조회
     * GET /api/product/top
     * @param period 조회 기간 (기본값: daily, 옵션: daily/weekly/monthly)
     * @param size 조회할 상품 개수 (기본값: 5, 최대: 50)
     * @return 인기 상품 목록 (기간별 판매량 기준)
     */
    @GetMapping("/top")
    public List<ProductListResponse> getTopProducts(
            @RequestParam(defaultValue = "daily") String period,
            @RequestParam(defaultValue = "5") int size) {
        return productService.getTopProducts(period, size).stream()
                .map(productMapper::toProductListResponse)
                .toList();
    }

    /**
     * 상품 옵션 등록
     * POST /api/product/option
     * @param request 상품 옵션 등록 요청
     * @return 등록된 상품 옵션
     */
    @PostMapping("/option")
    public ProductOptionResponse createProductOption(@Valid @RequestBody ProductOptionRegisterRequest request) {
        return productMapper.toProductOptionResponse(productService.createProductOption(
                request.getProductId(),
                request.getOptionName(),
                request.getPriceAdjustment(),
                request.getStockQuantity(),
                request.isExposed()
        ));
    }

    /**
     * 상품 옵션 수정
     * PUT /api/product/option/{productOptionId}
     * @param productOptionId 상품 옵션 ID
     * @param request 상품 옵션 수정 요청
     * @return 수정된 상품 옵션
     */
    @PutMapping("/option/{productOptionId}")
    public ProductOptionResponse updateProductOption(
            @PathVariable Long productOptionId,
            @Valid @RequestBody ProductOptionUpdateRequest request) {
        return productMapper.toProductOptionResponse(productService.updateProductOption(
                productOptionId,
                request.getOptionName(),
                request.getPriceAdjustment(),
                request.isExposed()
        ));
    }

    /**
     * 상품 옵션 삭제
     * DELETE /api/product/option/{productOptionId}
     * @param productOptionId 상품 옵션 ID
     */
    @DeleteMapping("/option/{productOptionId}")
    public void deleteProductOption(@PathVariable Long productOptionId) {
        productService.deleteProductOption(productOptionId);
    }
}
