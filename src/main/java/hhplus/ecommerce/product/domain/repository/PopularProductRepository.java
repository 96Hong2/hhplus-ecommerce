package hhplus.ecommerce.product.domain.repository;

import hhplus.ecommerce.product.domain.model.PopularProduct;

public interface PopularProductRepository {
    /**
     * 인기상품 집계 데이터 저장
     * @param popularProduct
     * @return 입기상품 집계 정보
     */
    PopularProduct save(PopularProduct popularProduct);
}
