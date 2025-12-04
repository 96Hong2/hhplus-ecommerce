package hhplus.ecommerce.product.application.service;

import hhplus.ecommerce.common.domain.constants.BusinessConstants;
import hhplus.ecommerce.common.domain.exception.ProductException;
import hhplus.ecommerce.common.presentation.response.PageResponse;
import hhplus.ecommerce.product.application.dto.ProductRankingDto;
import hhplus.ecommerce.product.domain.model.PeriodType;
import hhplus.ecommerce.product.domain.model.PopularProduct;
import hhplus.ecommerce.product.domain.model.Product;
import hhplus.ecommerce.product.domain.model.ProductOption;
import hhplus.ecommerce.product.domain.repository.PopularProductRepository;
import hhplus.ecommerce.product.domain.repository.ProductOptionRepository;
import hhplus.ecommerce.product.domain.repository.ProductRepository;
import hhplus.ecommerce.product.presentation.dto.response.ProductDetailResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductOptionRepository productOptionRepository;
    private final RedisTemplate<String, String> redisTemplate;
    private final PopularProductRepository popularProductRepository;

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
        } catch (ProductException e) {
            // Product.create에서 발생한 ProductException은 그대로 전파
            throw e;
        } catch (Exception e) {
            // 기타 예외 (Repository 저장 시 발생)
            e.printStackTrace(); // 디버깅을 위한 스택트레이스 출력
            throw ProductException.creationFailed("상품 등록 중 오류: " + e.getClass().getSimpleName() + " - " + e.getMessage());
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
        org.springframework.data.domain.Page<Product> productPage;
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(page, size);

        productPage = switch (sort) {
            case "latest" -> productRepository.findByCategoryOrderByCreatedAtDesc(category, pageable);
            case "sales" -> productRepository.findAll(pageable); // TODO: 판매량순 구현 필요
            case "price_asc" -> productRepository.findAllByPriceAsc(pageable);
            case "price_desc" -> productRepository.findAllByPriceDesc(pageable);
            default -> throw ProductException.getListFailed("지원하지 않는 정렬 기준입니다: " + sort);
        };

        return new PageResponse<>(
                productPage.getContent(),
                page,
                size,
                productPage.getTotalElements(),
                productPage.getTotalPages()
        );
    }

    /**
     * 상품 상세 조회 (옵션 포함)
     * Redis 캐싱 적용: TTL 30분
     * @param productId 상품 ID
     * @return 상품 상세 정보
     */
    @Cacheable(value = "productDetail", key = "#productId")
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
                productOptionList.stream().map(po -> new ProductMapper().toProductOptionResponse(po)).toList()
        );
    }

    /**
     * 상품 수정
     * 상품 수정 시 캐시 무효화
     * @param productId 상품 ID
     * @param productName 상품명
     * @param category 카테고리
     * @param description 상품 설명
     * @param imageUrl 이미지 URL
     * @param price 상품 기본 가격
     * @param isExposed 노출 여부
     * @return 수정된 상품
     */
    @CacheEvict(value = "productDetail", key = "#productId")
    public Product updateProduct(Long productId, String productName, String category,
                                String description, String imageUrl, BigDecimal price, long salesCount, boolean isExposed) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> ProductException.productNotFound(productId, productName));

        product.update(productName, category, description, imageUrl, price, salesCount, isExposed);

        return productRepository.save(product);
    }

    /**
     * 상품 삭제 (논리 삭제)
     * 상품 삭제 시 캐시 무효화
     * @param productId 상품 ID
     */
    @CacheEvict(value = "productDetail", key = "#productId")
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
     * 여러 상품 옵션 ID로 배치 조회 (N+1 문제 해결)
     * @param productOptionIds 상품 옵션 ID 목록
     * @return 상품 옵션 목록
     */
    public List<ProductOption> getProductOptionsByIds(List<Long> productOptionIds) {
        if (productOptionIds == null || productOptionIds.isEmpty()) {
            return List.of();
        }
        return productOptionRepository.findAllById(productOptionIds);
    }

    /**
     * 여러 상품 ID로 상품 정보 배치 조회 (N+1 문제 해결)
     * @param productIds 상품 ID 목록
     * @return 상품 ID를 키로 하는 ProductDetailResponse Map
     */
    public Map<Long, ProductDetailResponse> getProductDetailsByIds(List<Long> productIds) {
        if (productIds == null || productIds.isEmpty()) {
            return Map.of();
        }

        // 상품 엔티티 조회
        List<Product> products = productRepository.findAllById(productIds);

        // 각 상품에 대한 옵션 조회
        List<ProductOption> allOptions = productOptionRepository.findAllByProductIdIn(productIds);

        // productId별로 옵션 그룹화
        Map<Long, List<ProductOption>> optionsByProductId = allOptions.stream()
                .collect(Collectors.groupingBy(ProductOption::getProductId));

        // ProductDetailResponse로 변환
        return products.stream()
                .collect(Collectors.toMap(
                        Product::getProductId,
                        product -> {
                            List<ProductOption> options = optionsByProductId.getOrDefault(
                                    product.getProductId(),
                                    List.of()
                            );
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
                                    options.stream()
                                            .map(po -> new ProductMapper().toProductOptionResponse(po))
                                            .toList()
                            );
                        }
                ));
    }

    /**
     * 인기 상품 조회 (기간별 판매량 기준)
     *
     * Redis 실시간 랭킹 조회 전략:
     * - daily: 오늘 판매량 기준
     * - weekly: 최근 7일 판매량 기준
     * - monthly: 이번 달 판매량 기준
     *
     * @param period 조회 기간 ("daily", "weekly", "monthly")
     * @param limit 조회할 상품 개수 (Top N)
     * @return 인기 상품 랭킹 목록 (순위 포함)
     */
    @Cacheable(value = "popularProducts", key = "#period + ':' + #limit")
    public List<ProductRankingDto> getTopProducts(String period, int limit) {

        if (limit > BusinessConstants.MAX_RANK || limit <= 0) {
            throw ProductException.getListFailed(
                String.format("조회할 상품 수는 1 이상 %d개 이하여야 합니다. 입력값: %d",
                    BusinessConstants.MAX_RANK, limit)
            );
        }

        // 기간에 따라 Redis 키 선택
        String redisKey = switch (period.toLowerCase()) {
            case "daily" -> BusinessConstants.REDIS_TOP_N_DAILY_KEY;
            case "weekly" -> BusinessConstants.REDIS_TOP_N_WEEKLY_KEY;
            case "monthly" -> BusinessConstants.REDIS_TOP_N_MONTHLY_KEY;
            default -> throw ProductException.getListFailed("지원하지 않는 기간입니다: " + period + " (daily, weekly, monthly만 가능)");
        };

        // Redis ZSet에서 score 내림차순으로 상위 limit 개 조회
        Set<ZSetOperations.TypedTuple<String>> ranking =
            redisTemplate.opsForZSet()
                .reverseRangeWithScores(redisKey, 0, limit - 1);

        // Redis에 데이터가 없을 경우 DB에서 조회 (백업 전략)
        if (ranking == null || ranking.isEmpty()) {
            PeriodType periodType = switch (period.toLowerCase()) {
                case "daily" -> PeriodType.DAILY;
                case "weekly" -> PeriodType.WEEKLY;
                case "monthly" -> PeriodType.MONTHLY;
                default -> throw ProductException.getListFailed("지원하지 않는 기간입니다: " + period);
            };

            // DB에서 가장 최근 집계된 인기 상품 조회
            List<PopularProduct> popularProducts = popularProductRepository.findTopNByPeriodType(
                periodType,
                org.springframework.data.domain.PageRequest.of(0, limit)
            );

            // DB에도 데이터가 없으면 빈 리스트 반환
            if (popularProducts.isEmpty()) {
                return List.of();
            }

            // PopularProduct에서 productId 추출하여 상품 정보 조회
            List<Long> productIds = popularProducts.stream()
                .map(PopularProduct::getProductId)
                .collect(Collectors.toList());

            Map<Long, Product> productMap = productRepository
                .findAllById(productIds).stream()
                .collect(Collectors.toMap(Product::getProductId, p -> p));

            // ProductRankingDto 변환
            return popularProducts.stream()
                .map(pp -> {
                    Product product = productMap.get(pp.getProductId());
                    if (product == null) {
                        return null;
                    }
                    return new ProductRankingDto(
                        product.getProductId(),
                        product.getProductName(),
                        product.getImageUrl(),
                        pp.getSalesCount(),
                        pp.getRank()
                    );
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        }

        // Redis에서 조회한 productId로 DB에서 상품 정보 배치 조회 (N+1 방지)
        List<Long> productIds = ranking.stream()
            .map(tuple -> Long.parseLong(tuple.getValue()))
            .collect(Collectors.toList());

        Map<Long, Product> productMap = productRepository
            .findAllById(productIds).stream()
            .collect(Collectors.toMap(Product::getProductId, p -> p));

        // ProductRankingDto 변환
        AtomicInteger rank = new AtomicInteger(1);
        return ranking.stream()
            .map(tuple -> {
                Long productId = Long.parseLong(tuple.getValue());
                Product product = productMap.get(productId);

                if (product == null) {
                    return null;
                }

                rank.getAndIncrement();

                return new ProductRankingDto(
                    product.getProductId(),
                    product.getProductName(),
                    product.getImageUrl(),
                    tuple.getScore().intValue(), // 판매량
                    rank.get() // 순위 (1위, 2위, ...)
                );
            })
            .filter(Objects::nonNull) // null 아닌 값들만 리스트에 넣음
            .collect(Collectors.toList());
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
     * 옵션 수정 시 해당 상품의 캐시 무효화
     * @param productOptionId 상품 옵션 ID
     * @param optionName 옵션명
     * @param priceAdjustment 옵션 가격 조정값
     * @param isExposed 노출 여부
     * @return 수정된 상품 옵션
     */
    @CacheEvict(value = "productDetail", key = "#result.productId")
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
