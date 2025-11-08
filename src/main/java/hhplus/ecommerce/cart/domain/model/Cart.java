package hhplus.ecommerce.cart.domain.model;

import hhplus.ecommerce.common.domain.exception.CartException;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicLong;

@Getter
public class Cart {

    private static AtomicLong sequence = new AtomicLong(1);

    private final Long cartId;
    private final Long userId;
    private final Long productOptionId;
    private int quantity;
    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private Cart(Long cartId, Long userId, Long productOptionId, int quantity) {
        this.cartId = cartId;
        this.userId = userId;
        this.productOptionId = productOptionId;
        this.quantity = quantity;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
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

        Long id = sequence.getAndIncrement();
        return new Cart(id, userId, productOptionId, quantity);
    }

    /**
     * 장바구니 수량을 변경한다.
     * @param quantity 변경할 수량
     */
    public void updateQuantity(int quantity) {
        validateQuantity(quantity);
        this.quantity = quantity;
        this.updatedAt = LocalDateTime.now();
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
        this.updatedAt = LocalDateTime.now();
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
