package hhplus.ecommerce.cart.domain.model;

import hhplus.ecommerce.common.domain.exception.CartException;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "carts", indexes = {
    @Index(name = "idx_user_product_option", columnList = "user_id, product_option_id", unique = true),
    @Index(name = "idx_user_id", columnList = "user_id")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Cart {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long cartId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "product_option_id", nullable = false)
    private Long productOptionId;

    @Column(name = "quantity", nullable = false)
    private int quantity;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    private Cart(Long cartId, Long userId, Long productOptionId, int quantity) {
        this.cartId = cartId;
        this.userId = userId;
        this.productOptionId = productOptionId;
        this.quantity = quantity;
    }

    /**
     * 장바구니 아이템을 생성한다.
     * @param userId 사용자 ID
     * @param productOptionId 상품 옵션 ID
     * @param quantity 수량
     * @return 생성된 장바구니 아이템
     */
    public static Cart create(Long userId, Long productOptionId, int quantity) {
        validateUserId(userId);
        validateProductOptionId(productOptionId);
        validateQuantity(quantity);

        return new Cart(null, userId, productOptionId, quantity);
    }

    /**
     * 장바구니 수량을 변경한다.
     * @param quantity 변경할 수량
     */
    public void updateQuantity(int quantity) {
        validateQuantity(quantity);
        this.quantity = quantity;
    }

    /**
     * 장바구니 수량을 추가한다. (동일 옵션 추가 시)
     * @param additionalQuantity 추가할 수량
     */
    public void addQuantity(int additionalQuantity) {
        if (additionalQuantity <= 0) {
            throw CartException.invalidCartQuantity(additionalQuantity);
        }
        this.quantity += additionalQuantity;
    }

    private static void validateUserId(Long userId) {
        if (userId == null) {
            throw CartException.cartAddFailed("사용자 ID는 필수입니다.");
        }
    }

    private static void validateProductOptionId(Long productOptionId) {
        if (productOptionId == null) {
            throw CartException.cartAddFailed("상품 옵션 ID는 필수입니다.");
        }
    }

    private static void validateQuantity(int quantity) {
        if (quantity <= 0) {
            throw CartException.invalidCartQuantity(quantity);
        }
    }
}
