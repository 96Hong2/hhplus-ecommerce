package hhplus.ecommerce.product.domain.repository;

import hhplus.ecommerce.product.domain.model.ProductOption;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProductOptionRepository extends JpaRepository<ProductOption, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT po FROM ProductOption po WHERE po.productOptionId = :productOptionId")
    Optional<ProductOption> findByIdWithLock(@Param("productOptionId") Long productOptionId);

    @Query("SELECT po FROM ProductOption po WHERE po.productId = :productId AND po.isExposed = true AND po.isDeleted = false")
    List<ProductOption> findByProductId(@Param("productId") Long productId);

    @Query("SELECT po FROM ProductOption po WHERE po.productId = :productId AND po.isExposed = true AND po.isSoldOut = false AND po.isDeleted = false")
    List<ProductOption> findAvailableByProductId(@Param("productId") Long productId);

    List<ProductOption> findAllByProductId(Long productId);

    // 조건부 감소: 재고가 충분할 때만 감소 (원자적 DML)
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE ProductOption po SET po.stockQuantity = po.stockQuantity - :qty WHERE po.productOptionId = :id AND po.stockQuantity >= :qty")
    int decreaseIfEnough(@Param("id") Long productOptionId, @Param("qty") int quantity);

    // 증가: 해제/복구 시 사용
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE ProductOption po SET po.stockQuantity = po.stockQuantity + :qty WHERE po.productOptionId = :id")
    int increaseStock(@Param("id") Long productOptionId, @Param("qty") int quantity);
}
