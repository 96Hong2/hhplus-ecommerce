package hhplus.ecommerce.product.domain.model;

import hhplus.ecommerce.common.domain.constants.BusinessConstants;
import hhplus.ecommerce.common.domain.exception.StockException;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicLong;

@Getter
public class StockHistory {
    private static final AtomicLong sequence = new AtomicLong(1);

    private final Long stockHistoryId;
    private final Long productOptionId;
    private final int amount;
    private final int balance;
    private final String description;
    private final Long updatedBy;
    private final LocalDateTime createdAt;

    private StockHistory(Long stockHistoryId, Long productOptionId, int amount, int balance, String description, Long updatedBy) {
        this.stockHistoryId = stockHistoryId;
        this.productOptionId = productOptionId;
        this.amount = amount;
        this.balance = balance;
        this.description = description;
        this.updatedBy = updatedBy;
        this.createdAt = LocalDateTime.now();
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

    public static StockHistory create(Long productOptionId, int amount, int balance, String description, Long updatedBy) {
        validateProductOptionId(productOptionId);
        validateBalance(balance);
        validateDescription(description);

        return new StockHistory(sequence.getAndIncrement(), productOptionId, amount, balance, description, updatedBy);
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
        return create(productOptionId, amount, balance, description, updatedBy);
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
        return create(productOptionId, -amount, balance, description, updatedBy);
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
