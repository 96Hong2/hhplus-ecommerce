package hhplus.ecommerce.product.infrastructure.repository;

import hhplus.ecommerce.product.domain.model.StockHistory;
import hhplus.ecommerce.product.domain.model.StockAdjustmentType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface StockHistoryJpaRepository extends JpaRepository<StockHistory, Long> {

    // 상품 옵션별 재고 이력 조회 (최신순)
    @Query("SELECT sh FROM StockHistory sh WHERE sh.productOptionId = :productOptionId ORDER BY sh.createdAt DESC")
    List<StockHistory> findByProductOptionIdOrderByCreatedAtDesc(@Param("productOptionId") Long productOptionId);

    // 상품 옵션별 + 조정 타입별 재고 이력 조회
    @Query("SELECT sh FROM StockHistory sh WHERE sh.productOptionId = :productOptionId AND sh.adjustmentType = :adjustmentType ORDER BY sh.createdAt DESC")
    List<StockHistory> findByProductOptionIdAndAdjustmentType(@Param("productOptionId") Long productOptionId,
                                                               @Param("adjustmentType") StockAdjustmentType adjustmentType);
}
