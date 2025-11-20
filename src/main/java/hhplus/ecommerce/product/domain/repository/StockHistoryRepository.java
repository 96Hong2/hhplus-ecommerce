package hhplus.ecommerce.product.domain.repository;

import hhplus.ecommerce.product.domain.model.StockAdjustmentType;
import hhplus.ecommerce.product.domain.model.StockHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface StockHistoryRepository extends JpaRepository<StockHistory, Long> {

    @Query("SELECT sh FROM StockHistory sh WHERE sh.productOptionId = :productOptionId ORDER BY sh.createdAt DESC")
    List<StockHistory> findByProductOptionIdOrderByCreatedAtDesc(@Param("productOptionId") Long productOptionId);

    @Query("SELECT sh FROM StockHistory sh WHERE sh.productOptionId = :productOptionId AND sh.adjustmentType = :adjustmentType ORDER BY sh.createdAt DESC")
    List<StockHistory> findByProductOptionIdAndAdjustmentType(@Param("productOptionId") Long productOptionId,
                                                               @Param("adjustmentType") StockAdjustmentType adjustmentType);

    Page<StockHistory> findByProductOptionIdOrderByCreatedAtDesc(Long productOptionId, Pageable pageable);
}
