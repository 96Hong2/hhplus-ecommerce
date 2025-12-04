package hhplus.ecommerce.product.domain.model;

import hhplus.ecommerce.common.domain.constants.BusinessConstants;
import hhplus.ecommerce.common.domain.exception.ProductException;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 *
 * 역할: 인기상품 랭킹 스냅샷 저장 (히스토리/분석용)
 *
 * 활용 방안:
 * 1. 스케줄러로 매일 자정 Redis → DB 스냅샷 저장
 * 2. 히스토리 조회: "최근 7일간 특정 상품의 순위 변화"
 * 3. 트렌드 분석: "이번 주 급상승 상품"
 * 4. Redis 장애 시 백업 데이터로 활용
 * 5. 리포트/대시보드용 집계 데이터
 *
 * Redis vs DB 역할 분리:
 * - Redis: 실시간 랭킹 조회 (빠른 읽기)
 * - DB (PopularProduct): 히스토리 저장 (분석/백업)
 */

@Entity
@Table(name = "popular_products", indexes = {
    @Index(name = "idx_calculation_rank", columnList = "calculation_date, period_type, rank", unique = true),
    @Index(name = "idx_calculation_product_id", columnList = "calculation_date, product_id"),
    @Index(name = "idx_calculation_date", columnList = "calculation_date")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PopularProduct {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long popularProductId;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "sales_count", nullable = false)
    private int salesCount;

    @Column(name = "calculation_date", nullable = false)
    private LocalDate calculationDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "period_type", nullable = false)
    private PeriodType periodType;

    @Column(name = "rank", nullable = false)
    private int rank;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private PopularProduct(Long popularProductId, Long productId, int salesCount, LocalDate calculationDate, PeriodType periodType, int rank) {
        this.popularProductId = popularProductId;
        this.productId = productId;
        this.salesCount = salesCount;
        this.calculationDate = calculationDate;
        this.periodType = periodType;
        this.rank = rank;
    }

    /**
     * 인기 상품을 생성한다.
     * @param productId 상품 ID
     * @param salesCount 판매 수량
     * @param calculationDate 집계 날짜
     * @param rank 순위
     * @return 생성된 인기 상품
     */
    public static PopularProduct create(Long productId, int salesCount, LocalDate calculationDate, PeriodType periodType, int rank) {
        validateProductId(productId);
        validateSalesCount(salesCount);
        validateCalculationDate(calculationDate);
        validateRank(rank);

        return new PopularProduct(null, productId, salesCount, calculationDate, periodType, rank);
    }

    private static void validateProductId(Long productId) {
        if (productId == null) {
            throw ProductException.creationFailed("상품 ID는 필수입니다.");
        }
    }

    private static void validateSalesCount(int salesCount) {
        if (salesCount < 0) {
            throw new IllegalArgumentException("판매 수량은 0보다 작을 수 없습니다.");
        }
    }

    private static void validateCalculationDate(LocalDate calculationDate) {
        if (calculationDate == null) {
            throw new IllegalArgumentException("집계 날짜는 필수입니다.");
        }
        if (calculationDate.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("집계 날짜는 미래일 수 없습니다.");
        }
    }

    private static void validateRank(int rank) {
        if (rank <= 0) {
            throw new IllegalArgumentException("순위는 1 이상이어야 합니다.");
        }
        if (rank > BusinessConstants.MAX_RANK) {
            throw new IllegalArgumentException("순위는 " + BusinessConstants.MAX_RANK + " 이하여야 합니다.");
        }
    }

    /**
     * 상위 N위 안에 드는지 확인한다.
     * @param topN 기준 순위
     * @return 상위 N위 포함 여부
     */
    public boolean isTopN(int topN) {
        return rank <= topN;
    }

    /**
     * 집계 기간이 유효한지 확인한다. (최근 N일 이내)
     * @param days 기준 일수
     * @return 유효 여부
     */
    public boolean isRecent(int days) {
        return calculationDate.isAfter(LocalDate.now().minusDays(days));
    }
}
