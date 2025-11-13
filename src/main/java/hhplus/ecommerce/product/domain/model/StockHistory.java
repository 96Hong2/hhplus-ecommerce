package hhplus.ecommerce.product.domain.model;

import hhplus.ecommerce.common.domain.constants.BusinessConstants;
import hhplus.ecommerce.common.domain.exception.StockException;
import jakarta.persistence.*;
import hhplus.ecommerce.product.domain.model.StockAdjustmentType;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "stock_histories", indexes = {
    @Index(name = "idx_product_option_created", columnList = "product_option_id, created_at"),
    @Index(name = "idx_product_option_type_created", columnList = "product_option_id, adjustment_type, created_at"),
    @Index(name = "idx_product_option_updatedby_created", columnList = "product_option_id, updated_by, created_at"),
    @Index(name = "idx_created_at", columnList = "created_at")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StockHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long stockHistoryId;

    @Column(name = "product_option_id", nullable = false)
    private Long productOptionId;

    @Column(name = "amount", nullable = false)
    private int amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "adjustment_type", nullable = false, length = 20)
    private StockAdjustmentType adjustmentType;

    @Column(name = "balance", nullable = false)
    private int balance;

    @Column(name = "description", length = 200)
    private String description;

    @Column(name = "updated_by", nullable = false)
    private Long updatedBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private StockHistory(Long stockHistoryId, Long productOptionId, int amount, StockAdjustmentType adjustmentType, int balance, String description, Long updatedBy) {
        this.stockHistoryId = stockHistoryId;
        this.productOptionId = productOptionId;
        this.amount = amount;
        this.adjustmentType = adjustmentType;
        this.balance = balance;
        this.description = description;
        this.updatedBy = updatedBy;
    }

    /**
     * 재고 이력을 생성한다.
     * @param productOptionId 상품 옵션 ID
     * @param amount 변동량 (양수: 증가, 음수: 감소)
     * @param balance 변동 후 재고 잔액
     * @param description 설명
     * @param updatedBy 수정자 ID
     * @return 생성된 재고 이력
     */

    public static StockHistory create(Long productOptionId, int amount, StockAdjustmentType adjustmentType, int balance, String description, Long updatedBy) {
        validateProductOptionId(productOptionId);
        validateBalance(balance);
        validateDescription(description);

        return new StockHistory(null, productOptionId, amount, adjustmentType, balance, description, updatedBy);
    }

    private static void validateProductOptionId(Long productOptionId) {
        if (productOptionId == null) {
            throw StockException.stockReservationInvalidParameters(null, productOptionId, null);
        }
    }

    private static void validateBalance(int balance) {
        if (balance < 0) {
            throw StockException.invalidStockAmount(balance);
        }
    }

    private static void validateDescription(String description) {
        if (description != null && description.length() > BusinessConstants.MAX_STOCK_HISTORY_DESCRIPTION_LENGTH) {
            throw new IllegalArgumentException("설명은 " + BusinessConstants.MAX_STOCK_HISTORY_DESCRIPTION_LENGTH + "자를 초과할 수 없습니다.");
        }
    }

    /**
     * 재고 증가 이력을 생성한다.
     * @param productOptionId 상품 옵션 ID
     * @param amount 증가량
     * @param balance 변동 후 재고 잔액
     * @param description 설명
     * @param updatedBy 수정자 ID
     * @return 재고 증가 이력
     */
    public static StockHistory forIncrease(Long productOptionId, int amount, int balance, String description, Long updatedBy) {
        if (amount <= 0) {
            throw StockException.invalidStockAmount(amount);
        }
        return create(productOptionId, amount, StockAdjustmentType.ADD, balance, description, updatedBy);
    }

    /**
     * 재고 감소 이력을 생성한다.
     * @param productOptionId 상품 옵션 ID
     * @param amount 감소량 (양수)
     * @param balance 변동 후 재고 잔액
     * @param description 설명
     * @param updatedBy 수정자 ID
     * @return 재고 감소 이력
     */
    public static StockHistory forDecrease(Long productOptionId, int amount, int balance, String description, Long updatedBy) {
        if (amount <= 0) {
            throw StockException.invalidStockAmount(amount);
        }
        return create(productOptionId, -amount, StockAdjustmentType.USE, balance, description, updatedBy);
    }

    /**
     * 재고가 증가했는지 확인한다.
     * @return 증가 여부
     */
    public boolean isIncrease() {
        return amount > 0;
    }

    /**
     * 재고가 감소했는지 확인한다.
     * @return 감소 여부
     */
    public boolean isDecrease() {
        return amount < 0;
    }
}
