package hhplus.ecommerce.product.domain.model;

import hhplus.ecommerce.common.domain.constants.BusinessConstants;
import hhplus.ecommerce.common.domain.exception.ProductException;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicLong;

@Getter
public class PopularProduct {
    private static AtomicLong sequence = new AtomicLong(1);

    private final Long popularProductId;
    private final Long productId;
    private final int salesCount;
    private final LocalDateTime calculationDate;
    private final int rank;
    private final LocalDateTime createdAt;

    private PopularProduct(Long popularProductId, Long productId, int salesCount, LocalDateTime calculationDate, int rank) {
        this.popularProductId = popularProductId;
        this.productId = productId;
        this.salesCount = salesCount;
        this.calculationDate = calculationDate;
        this.rank = rank;
        this.createdAt = LocalDateTime.now();
    }

    /**
     * 인기 상품을 생성한다.
     * @param productId 상품 ID
     * @param salesCount 판매 수량
     * @param calculationDate 집계 날짜
     * @param rank 순위
     * @return 생성된 인기 상품
     */
    public static PopularProduct create(Long productId, int salesCount, LocalDateTime calculationDate, int rank) {
        validateProductId(productId);
        validateSalesCount(salesCount);
        validateCalculationDate(calculationDate);
        validateRank(rank);

        Long id = sequence.getAndIncrement();
        return new PopularProduct(id, productId, salesCount, calculationDate, rank);
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

    private static void validateCalculationDate(LocalDateTime calculationDate) {
        if (calculationDate == null) {
            throw new IllegalArgumentException("집계 날짜는 필수입니다.");
        }
        if (calculationDate.isAfter(LocalDateTime.now())) {
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
        return calculationDate.isAfter(LocalDateTime.now().minusDays(days));
    }
}
