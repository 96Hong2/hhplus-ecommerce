package hhplus.ecommerce.cart.domain.repository;

import hhplus.ecommerce.cart.domain.model.Cart;

import java.util.List;
import java.util.Optional;
public interface CartRepository {

    Cart save(Cart cart);

    Optional<Cart> findById(Long cartId);

    List<Cart> findByUserId(Long userId);

    Optional<Cart> findByUserIdAndProductOptionId(Long userId, Long productOptionId);

    void delete(Long cartId);

    void deleteByUserIdAndProductId(Long userId, Long productId);
}
