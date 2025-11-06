package hhplus.ecommerce.product.domain.repository;

import hhplus.ecommerce.product.domain.model.ProductOption;

import java.util.List;
import java.util.Optional;

public interface ProductOptionRepository {
    /**
     * 상품옵션 등록
     * @param productOption
     * @return 상품옵션
     */
    ProductOption save(ProductOption productOption);

    /**
     * 상품옵션 조회
     * @param productOptionId
     * @return 상품옵션
     */
    Optional<ProductOption> findById(Long productOptionId);

    /**
     * 상품 ID에 해당하는 상품 옵션 목록을 가져온다.
     * @param productId
     * @return 상품옵션 목록
     */
    List<ProductOption> findAllByProductId(Long productId);
}
