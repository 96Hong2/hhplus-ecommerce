package hhplus.ecommerce.product.infrastructure.repository;

import hhplus.ecommerce.product.domain.model.PopularProduct;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface PopularProductJpaRepository extends JpaRepository<PopularProduct, Long> {

    // 특정 날짜의 인기 상품 조회 (순위순)
    @Query("SELECT pp FROM PopularProduct pp WHERE pp.calculationDate = :date ORDER BY pp.rank ASC")
    List<PopularProduct> findByCalculationDateOrderByRankAsc(@Param("date") LocalDate date);

    // 최근 N일간 인기 상품 조회
    @Query("SELECT pp FROM PopularProduct pp WHERE pp.calculationDate >= :startDate ORDER BY pp.calculationDate DESC, pp.rank ASC")
    List<PopularProduct> findRecentPopularProducts(@Param("startDate") LocalDate startDate);

    // 특정 상품의 인기 상품 이력 조회
    @Query("SELECT pp FROM PopularProduct pp WHERE pp.productId = :productId ORDER BY pp.calculationDate DESC")
    List<PopularProduct> findByProductId(@Param("productId") Long productId);

    // 특정 날짜 범위의 인기 상품 삭제 (배치 정리용)
    void deleteByCalculationDateBefore(LocalDate date);
}
