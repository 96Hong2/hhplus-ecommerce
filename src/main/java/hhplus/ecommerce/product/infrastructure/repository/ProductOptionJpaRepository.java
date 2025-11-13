package hhplus.ecommerce.product.infrastructure.repository;

import hhplus.ecommerce.product.domain.model.ProductOption;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;

public interface ProductOptionJpaRepository extends JpaRepository<ProductOption, Long> {

    // 비관적 락으로 상품 옵션 조회 (재고 차감용)
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT po FROM ProductOption po WHERE po.productOptionId = :productOptionId")
    Optional<ProductOption> findByIdWithLock(@Param("productOptionId") Long productOptionId);

    // 상품별 옵션 목록 조회
    @Query("SELECT po FROM ProductOption po WHERE po.productId = :productId AND po.isExposed = true AND po.isDeleted = false")
    List<ProductOption> findByProductId(@Param("productId") Long productId);

    // 판매 가능한 옵션만 조회
    @Query("SELECT po FROM ProductOption po WHERE po.productId = :productId AND po.isExposed = true AND po.isSoldOut = false AND po.isDeleted = false")
    List<ProductOption> findAvailableByProductId(@Param("productId") Long productId);
}
