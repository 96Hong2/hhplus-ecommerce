package hhplus.ecommerce.product.domain.repository;

import hhplus.ecommerce.product.domain.model.StockHistory;

import java.util.List;

public interface StockHistoryRepository {

    /**
     * 재고 이력을 저장한다.
     * @param stockHistory
     * @return 재고 이력
     */
    StockHistory save(StockHistory stockHistory);

    /**
     * 특정 상품옵션의 재고 이력을 페이징과 함께 가져온다.
     * @param productOptionId
     * @param page
     * @param size
     * @return 재고 이력
     */
    List<StockHistory> findAllByProductOptionIdWithPaging(Long productOptionId, int page, int size);
}
