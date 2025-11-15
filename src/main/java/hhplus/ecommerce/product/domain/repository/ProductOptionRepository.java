package hhplus.ecommerce.product.domain.repository;

import hhplus.ecommerce.product.domain.model.ProductOption;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
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
}
