package hhplus.ecommerce.product.domain.repository;

import hhplus.ecommerce.common.presentation.response.PageResponse;
import hhplus.ecommerce.product.domain.model.PopularProduct;
import hhplus.ecommerce.product.domain.model.Product;

import java.util.List;
import java.util.Optional;

public interface ProductRepository {

    /**
     * 상품 등록
     * @param product
     * @return 상품
     */
    Product save(Product product);

    /**
     * 상품 조회
     * @param productId
     * @return 상품
     */
    Optional<Product> findById(Long productId);

    /**
     * @return 총 상품 개수
     */
    long countAll();

    /**
     * 페이징 적용하여 상품 목록 조회 (기본 최신순, 카테고리 필터 적용)
     * @param page
     * @param size
     * @param category
     * @return 상품 목록
     */
    PageResponse<Product> findAllByCategoryWithPage(int page, int size, String category);

    /**
     * 가격순으로 상품 목록 조회
     * @param page
     * @param size
     * @param isAsc true : 가격 낮은 순, false : 가격 높은 순
     * @return 상품 목록
     */
    PageResponse<Product> findAllByPriceWithPaging(int page, int size, boolean isAsc);

    /**
     * 판매량순으로 상품 목록 조회
     * @param page
     * @param size
     * @return 상품 목록 (판매량 높은 순)
     */
    PageResponse<Product> findAllBySalesWithPaging(int page, int size);

    /**
     * 인기 상품 조회 (최근 집계일수간 판매량 기준)
     * @param TopN 조회할 상품 개수
     * @param searchDays 집계 일수
     * @return 인기 상품 목록
     */
    List<Product> findTopN(int TopN, int searchDays);
}
