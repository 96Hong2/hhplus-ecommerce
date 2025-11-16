package hhplus.ecommerce.product.domain.model;

import hhplus.ecommerce.common.domain.constants.BusinessConstants;
import hhplus.ecommerce.common.domain.exception.ProductException;
import hhplus.ecommerce.common.domain.exception.StockException;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "product_options", indexes = {
    @Index(name = "idx_product_id", columnList = "product_id"),
    @Index(name = "idx_product_soldout_exposed_deleted", columnList = "product_id, is_sold_out, is_exposed, is_deleted"),
    @Index(name = "idx_created_at", columnList = "created_at")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProductOption {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long productOptionId;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "option_name", nullable = false, length = 100)
    private String optionName;

    @Column(name = "price_adjustment", nullable = false, precision = 15, scale = 2)
    private BigDecimal priceAdjustment; // 옵션으로 인해 추가되거나 감소되는 가격

    @Column(name = "stock_quantity", nullable = false)
    private int stockQuantity;

    @Column(name = "is_exposed", nullable = false, columnDefinition = "TINYINT(1)")
    private boolean isExposed;

    @Column(name = "is_sold_out", nullable = false, columnDefinition = "TINYINT(1)")
    private boolean isSoldOut; // 명시적 품절 플래그 (재고 0 또는 수동 품절)

    @Column(name = "is_deleted", nullable = false, columnDefinition = "TINYINT(1)")
    private boolean isDeleted;   // 논리 삭제

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    private ProductOption(Long productOptionId, Long productId, String optionName, BigDecimal priceAdjustment, int stockQuantity, boolean isExposed) {
        this.productOptionId = productOptionId;
        this.productId = productId;
        this.optionName = optionName;
        this.priceAdjustment = priceAdjustment;
        this.stockQuantity = stockQuantity;
        this.isExposed = isExposed;
        this.isSoldOut = stockQuantity == 0;
        this.isDeleted = false;
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
        // priceAdjustment가 null이면 안되지만, 0원은 허용 (가격 변동 없는 옵션)
        if (priceAdjustment == null) {
            throw ProductException.creationFailed("옵션 변동 가격은 필수입니다. (0원 허용)");
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
        this.isSoldOut = this.stockQuantity == 0 || this.isSoldOut;
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
        this.isSoldOut = (this.stockQuantity == 0) || this.isSoldOut;
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
        if (this.stockQuantity > 0) {
            this.isSoldOut = false;
        }
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
        return isSoldOut || stockQuantity == 0;
    }

    /**
     * 구매 가능한지 확인한다.
     * @param requestedQuantity 요청 수량
     * @return 구매 가능 여부
     */
    public boolean isAvailableForPurchase(int requestedQuantity) {
        return isExposed && !isDeleted && !isSoldOut() && hasEnoughStock(requestedQuantity);
    }

    /**
     * 재고 수량을 직접 설정한다. (재고 조정용)
     * @param stockQuantity 설정할 재고 수량
     */
    public void setStockQuantity(int stockQuantity) {
        validateStockQuantity(stockQuantity);
        this.stockQuantity = stockQuantity;
        this.isSoldOut = (stockQuantity == 0) || this.isSoldOut;
    }

    /**
     * 상품옵션을 삭제한다. (비노출 처리)
     */
    public void hide() {
        this.isExposed = false;
    }

    /**
     * 옵션을 수동 품절 처리한다.
     */
    public void markSoldOut() {
        this.isSoldOut = true;
    }

    /**
     * 옵션을 논리 삭제한다.
     */
    public void delete() {
        this.isDeleted = true;
        this.isExposed = false;
    }
}
