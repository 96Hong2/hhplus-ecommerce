package hhplus.ecommerce.product.domain.model;

import hhplus.ecommerce.common.domain.exception.ProductException;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "products", indexes = {
    @Index(name = "idx_exposed_deleted_category", columnList = "is_exposed, is_deleted, category"),
    @Index(name = "idx_exposed_deleted_created", columnList = "is_exposed, is_deleted, created_at")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long productId;

    @Column(name = "product_name", nullable = false, length = 200)
    private String productName;

    @Column(name = "category", nullable = false, length = 100)
    private String category;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    // 기본 가격
    @Column(name = "price", nullable = false, precision = 15, scale = 2)
    private BigDecimal price;

    // 판매량은 popular_products 테이블에서 별도 관리 (통계용)
    @Transient
    private long salesCount;

    @Column(name = "is_exposed", nullable = false, columnDefinition = "TINYINT(1)")
    private boolean isExposed;

    @Column(name = "is_deleted", nullable = false, columnDefinition = "TINYINT(1)")
    private boolean isDeleted;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // 일반 생성자 (도메인 로직용)
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

        return new Product(null, productName.trim(), category.trim(), description, imageUrl, price, 0, isExposed);
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
    }

    /**
     * 상품명을 변경한다.
     */
    public void updateProductName(String productName) {
        validateProductName(productName);
        this.productName = productName.trim();
    }

    /**
     * 상품 노출 여부를 변경한다.
     */
    public void updateExposure(boolean isExposed) {
        this.isExposed = isExposed;
    }

    /**
     * 상품 이미지를 변경한다.
     */
    public void updateImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
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
    }
}
