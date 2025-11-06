package hhplus.ecommerce.product.domain.model;

import hhplus.ecommerce.common.domain.exception.ProductException;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicLong;

@Getter
public class Product {

    static AtomicLong sequence = new AtomicLong(1);

    private final Long productId;
    private String productName;
    private String category;
    private String description;
    private String imageUrl;
    private BigDecimal price;
    private long salesCount; // 판매량 (주문 완료 시 증가)
    private boolean isExposed;
    private boolean isDeleted;
    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private Product(Long productId, String productName, String category, String description, String imageUrl, BigDecimal price, long salesCount, boolean isExposed) {
        this.productId = productId;
        this.productName = productName;
        this.category = category;
        this.description = description;
        this.imageUrl = imageUrl;
        this.price = price;
        this.salesCount = salesCount;
        this.isExposed = isExposed;
        this.isDeleted = false;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 상품을 등록한다.
     * @param productName 상품명
     * @param category 카테고리
     * @param description 상품 설명
     * @param imageUrl 이미지 URL
     * @param price 상품 기본 가격
     * @param isExposed 노출 여부
     * @return 등록한 상품
     */
    public static Product create(String productName, String category, String description, String imageUrl, BigDecimal price, boolean isExposed) {
        validateProductName(productName);
        validateCategory(category);
        validatePrice(price);

        Long id = sequence.getAndIncrement();

        return new Product(id, productName.trim(), category.trim(), description, imageUrl, price, 0, isExposed);
    }

    private static void validateProductName(String productName) {
        if (productName == null || productName.trim().isEmpty()) {
            throw ProductException.creationFailed("상품명은 필수입니다.");
        }
    }

    private static void validateCategory(String category) {
        if (category == null || category.trim().isEmpty()) {
            throw ProductException.creationFailed("카테고리는 필수입니다.");
        }
    }

    private static void validatePrice(BigDecimal price) {
        if (price == null || price.compareTo(BigDecimal.ZERO) < 0) {
            throw ProductException.creationFailed("상품 가격은 0원 이상이어야 합니다.");
        }
    }

    /**
     * 상품 정보를 업데이트한다.
     * @param productName 상품명
     * @param category 카테고리
     * @param description 상품 설명
     * @param imageUrl 이미지 URL
     * @param price 상품 기본 가격
     * @param isExposed 노출 여부
     */
    public void update(String productName, String category, String description, String imageUrl, BigDecimal price, long salesCount, boolean isExposed) {
        validateProductName(productName);
        validateCategory(category);
        validatePrice(price);

        this.productName = productName.trim();
        this.category = category.trim();
        this.description = description;
        this.imageUrl = imageUrl;
        this.price = price;
        this.salesCount = salesCount;
        this.isExposed = isExposed;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 상품명을 변경한다.
     */
    public void updateProductName(String productName) {
        validateProductName(productName);
        this.productName = productName.trim();
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 상품 노출 여부를 변경한다.
     */
    public void updateExposure(boolean isExposed) {
        this.isExposed = isExposed;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 상품 이미지를 변경한다.
     */
    public void updateImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 상품이 노출 가능한지 확인한다.
     */
    public boolean canBeDisplayed() {
        return isExposed && !isDeleted;
    }

    /**
     * 상품을 삭제한다. (논리적 삭제)
     */
    public void delete() {
        this.isDeleted = true;
        this.isExposed = false;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 판매량을 증가시킨다. (주문 완료 시 호출)
     * @param quantity 증가시킬 수량
     */
    public void increaseSalesCount(long quantity) {
        if (quantity <= 0) {
            throw ProductException.creationFailed("증가 수량은 0보다 커야 합니다.");
        }
        this.salesCount += quantity;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 판매량을 감소시킨다. (주문 취소 시 호출)
     * @param quantity 감소시킬 수량
     */
    public void decreaseSalesCount(long quantity) {
        if (quantity <= 0) {
            throw ProductException.creationFailed("감소 수량은 0보다 커야 합니다.");
        }
        if (this.salesCount < quantity) {
            throw ProductException.creationFailed("판매량이 감소 수량보다 적습니다.");
        }
        this.salesCount -= quantity;
        this.updatedAt = LocalDateTime.now();
    }
}
