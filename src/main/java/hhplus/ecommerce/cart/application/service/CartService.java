package hhplus.ecommerce.cart.application.service;

import hhplus.ecommerce.common.domain.exception.CartException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import hhplus.ecommerce.cart.domain.model.Cart;
import hhplus.ecommerce.cart.domain.repository.CartRepository;
import hhplus.ecommerce.product.domain.model.ProductOption;
import hhplus.ecommerce.product.domain.repository.ProductOptionRepository;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CartService {
    private final CartRepository cartRepository;
    private final ProductOptionRepository productOptionRepository;

    /**
     * 장바구니 추가 (동일 옵션 시 수량 합산)
     */
    public Cart addToCart(Long userId, Long productOptionId, Integer quantity) {
        // 상품 옵션 존재 여부 확인
        ProductOption productOption = productOptionRepository.findById(productOptionId)
                .orElseThrow(() -> CartException.cartAddFailed("상품 옵션을 찾을 수 없습니다."));

        // 품절 체크
        if (productOption.isSoldOut()) {
            throw CartException.cartAddFailed("품절된 상품은 장바구니에 추가할 수 없습니다.");
        }

        // 기존 장바구니 항목 확인
        Optional<Cart> existingCart = cartRepository.findByUserIdAndProductOptionId(userId, productOptionId);

        if (existingCart.isPresent()) {
            // 기존 항목이 있으면 수량 합산
            Cart cart = existingCart.get();
            cart.updateQuantity(cart.getQuantity() + quantity);
            return cartRepository.save(cart);
        } else {
            // 새 항목 추가
            Cart newCart = Cart.create(
                    userId,
                    productOptionId,
                    quantity
            );
            return cartRepository.save(newCart);
        }
    }

    /**
     * 장바구니 수량 수정
     */
    public Cart updateCartQuantity(Long cartId, Integer quantity) {
        Cart cart = cartRepository.findById(cartId)
                .orElseThrow(() -> (CartException.cartUpdateFailed(cartId, "장바구니 항목을 찾을 수 없습니다.")));

        if (quantity < 1) {
            throw CartException.cartUpdateFailed(cartId, "수량은 1개 이상이어야 합니다.");
        }

        cart.updateQuantity(quantity);
        return cartRepository.save(cart);
    }

    /**
     * 장바구니 조회
     */
    public List<Cart> getCartItems(Long userId) {
        return cartRepository.findByUserId(userId);
    }

    /**
     * 장바구니 항목 삭제
     */
    public void removeFromCart(Long cartId) {
        if (cartRepository.findById(cartId).isEmpty()) {
            throw CartException.cartItemNotFound(cartId);
        }
        cartRepository.delete(cartId);
    }

    /**
     * 사용자의 특정 상품 전체 삭제
     */
    public void removeByUserIdAndProductId(Long userId, Long productId) {
        cartRepository.deleteByUserIdAndProductId(userId, productId);
    }
}
