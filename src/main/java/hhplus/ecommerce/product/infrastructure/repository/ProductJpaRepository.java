package hhplus.ecommerce.product.infrastructure.repository;

import hhplus.ecommerce.product.domain.model.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ProductJpaRepository extends JpaRepository<Product, Long> {

    // 노출되고 삭제되지 않은 상품 조회
    @Query("SELECT p FROM Product p WHERE p.productId = :productId AND p.isExposed = true AND p.isDeleted = false")
    Optional<Product> findByIdAndExposed(@Param("productId") Long productId);

    // 카테고리별 상품 조회 (노출된 것만, 최신순)
    @Query("SELECT p FROM Product p WHERE p.category = :category AND p.isExposed = true AND p.isDeleted = false ORDER BY p.createdAt DESC")
    Page<Product> findByCategoryOrderByCreatedAtDesc(@Param("category") String category, Pageable pageable);

    // 가격 오름차순
    @Query("SELECT p FROM Product p WHERE p.isExposed = true AND p.isDeleted = false ORDER BY p.price ASC")
    Page<Product> findAllByPriceAsc(Pageable pageable);

    // 가격 내림차순
    @Query("SELECT p FROM Product p WHERE p.isExposed = true AND p.isDeleted = false ORDER BY p.price DESC")
    Page<Product> findAllByPriceDesc(Pageable pageable);

    // 노출된 상품 수
    @Query("SELECT COUNT(p) FROM Product p WHERE p.isExposed = true AND p.isDeleted = false")
    long countAllExposed();
}
