package hhplus.ecommerce.product.domain.repository;

import hhplus.ecommerce.product.domain.model.PeriodType;
import hhplus.ecommerce.product.domain.model.PopularProduct;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface PopularProductRepository extends JpaRepository<PopularProduct, Long> {

    /**
     * 특정 기간 타입의 가장 최신 집계 날짜에 해당하는 인기 상품 조회 (순위 오름차순)
     * Redis 장애 시 백업용 또는 초기 데이터 로드용
     *
     * @param periodType 기간 타입 (DAILY, WEEKLY, MONTHLY)
     * @param pageable 페이징 정보 (limit 설정용)
     * @return 인기 상품 목록 (최신 집계일 기준, rank 순)
     */
    @Query("SELECT p FROM PopularProduct p " +
           "WHERE p.periodType = :periodType " +
           "AND p.calculationDate = (" +
           "    SELECT MAX(p2.calculationDate) " +
           "    FROM PopularProduct p2 " +
           "    WHERE p2.periodType = :periodType" +
           ") " +
           "ORDER BY p.rank ASC")
    List<PopularProduct> findTopNByPeriodType(@Param("periodType") PeriodType periodType, Pageable pageable);

    void deleteByCalculationDateBefore(LocalDate date);
}
