package hhplus.ecommerce.product.domain.repository;

import hhplus.ecommerce.product.domain.model.PopularProduct;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface PopularProductRepository extends JpaRepository<PopularProduct, Long> {

    @Query("SELECT pp FROM PopularProduct pp WHERE pp.calculationDate = :date ORDER BY pp.rank ASC")
    List<PopularProduct> findByCalculationDateOrderByRankAsc(@Param("date") LocalDate date);

    @Query("SELECT pp FROM PopularProduct pp WHERE pp.calculationDate >= :startDate ORDER BY pp.calculationDate DESC, pp.rank ASC")
    List<PopularProduct> findRecentPopularProducts(@Param("startDate") LocalDate startDate);

    @Query("SELECT pp FROM PopularProduct pp WHERE pp.productId = :productId ORDER BY pp.calculationDate DESC")
    List<PopularProduct> findByProductId(@Param("productId") Long productId);

    void deleteByCalculationDateBefore(LocalDate date);
}
