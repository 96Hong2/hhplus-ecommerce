package hhplus.ecommerce.product.domain.model;

import hhplus.ecommerce.common.domain.constants.BusinessConstants;
import hhplus.ecommerce.common.domain.exception.ProductException;
import hhplus.ecommerce.common.domain.exception.StockException;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
public class ProductOption {

    private final Long productOptionId;
    private final Long productId;
    private String optionName;
    private BigDecimal priceAdjustment; // 옵션으로 인해 추가되거나 감소되는 가격
    private int stockQuantity;
    private boolean isExposed;
    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private ProductOption(Long productOptionId, Long productId, String optionName, BigDecimal priceAdjustment, int stockQuantity, boolean isExposed) {
        this.productOptionId = productOptionId;
        this.productId = productId;
        this.optionName = optionName;
        this.priceAdjustment = priceAdjustment;
        this.stockQuantity = stockQuantity;
        this.isExposed = isExposed;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 상품옵션을 생성한다.
     * @param productId 상품 ID
     * @param optionName 옵션명
     * @param priceAdjustment 옵션으로 변경되는 가격 (양수 : 추가되는 가격, 음수 : 감소되는 가격)
     * @param stockQuantity 재고 수량
     * @param isExposed 노출 여부
     * @return 생성된 상품옵션
     */
    public static ProductOption create(Long productId, String optionName, BigDecimal priceAdjustment, int stockQuantity, boolean isExposed) {
        validateProductId(productId);
        validateOptionName(optionName);
        validatePriceAdjustment(priceAdjustment);
        validateStockQuantity(stockQuantity);

        return new ProductOption(null, productId, optionName.trim(), priceAdjustment, stockQuantity, isExposed);
    }

    private static void validateProductId(Long productId) {
        if (productId == null) {
            throw ProductException.creationFailed("상품 ID는 필수입니다.");
        }
    }

    private static void validateOptionName(String optionName) {
        if (optionName == null || optionName.trim().isEmpty()) {
            throw ProductException.creationFailed("옵션명은 필수입니다.");
        }
        if (optionName.trim().length() > BusinessConstants.MAX_OPTION_NAME_LENGTH) {
            throw ProductException.creationFailed("옵션명은 " + BusinessConstants.MAX_OPTION_NAME_LENGTH + "자를 초과할 수 없습니다.");
        }
    }

    private static void validatePriceAdjustment(BigDecimal priceAdjustment) {
        if (priceAdjustment != null && priceAdjustment.equals(BigDecimal.ZERO) ) {
            throw ProductException.creationFailed("옵션 변동 가격은 0원이 될 수 없습니다.");
        }
    }

    private static void validateStockQuantity(int stockQuantity) {
        if (stockQuantity < 0) {
            throw StockException.invalidStockAmount(stockQuantity);
        }
    }

    /**
     * 상품옵션 정보를 수정한다.
     * @param optionName 옵션명
     * @param priceAdjustment 옵션 가격
     * @param isExposed 노출 여부
     */
    public void update(String optionName, BigDecimal priceAdjustment, boolean isExposed) {
        validateOptionName(optionName);
        validatePriceAdjustment(priceAdjustment);

        this.optionName = optionName.trim();
        this.priceAdjustment = priceAdjustment;
        this.isExposed = isExposed;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 재고를 차감한다.
     * @param quantity 차감할 수량
     */
    public void decreaseStock(int quantity) {
        if (quantity <= 0) {
            throw StockException.invalidStockAmount(quantity);
        }
        if (stockQuantity < quantity) {
            throw StockException.stockQuantityInsufficient(productOptionId, stockQuantity, quantity);
        }
        this.stockQuantity -= quantity;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 재고를 증가시킨다.
     * @param quantity 증가할 수량
     */
    public void increaseStock(int quantity) {
        if (quantity <= 0) {
            throw StockException.invalidStockAmount(quantity);
        }
        this.stockQuantity += quantity;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 재고가 충분한지 확인한다.
     * @param requestedQuantity 요청 수량
     * @return 재고 충분 여부
     */
    public boolean hasEnoughStock(int requestedQuantity) {
        return stockQuantity >= requestedQuantity;
    }

    /**
     * 재고가 없는지 확인한다.
     * @return 품절 여부
     */
    public boolean isSoldOut() {
        return stockQuantity == 0;
    }

    /**
     * 구매 가능한지 확인한다.
     * @param requestedQuantity 요청 수량
     * @return 구매 가능 여부
     */
    public boolean isAvailableForPurchase(int requestedQuantity) {
        return isExposed && !isSoldOut() && hasEnoughStock(requestedQuantity);
    }

    /**
     * 재고 수량을 직접 설정한다. (재고 조정용)
     * @param stockQuantity 설정할 재고 수량
     */
    public void setStockQuantity(int stockQuantity) {
        validateStockQuantity(stockQuantity);
        this.stockQuantity = stockQuantity;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 상품옵션을 삭제한다. (비노출 처리)
     */
    public void hide() {
        this.isExposed = false;
        this.updatedAt = LocalDateTime.now();
    }
}