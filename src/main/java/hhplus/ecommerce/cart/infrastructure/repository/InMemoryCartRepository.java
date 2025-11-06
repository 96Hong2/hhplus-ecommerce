package hhplus.ecommerce.cart.infrastructure.repository;

import hhplus.ecommerce.cart.domain.model.Cart;
import hhplus.ecommerce.cart.domain.repository.CartRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Repository
public class InMemoryCartRepository implements CartRepository {
    private final ConcurrentHashMap<Long, Cart> storage = new ConcurrentHashMap<>();

    // 복합 인덱스: "userId:productOptionId" -> Cart ID
    private final ConcurrentHashMap<String, Long> userOptionIndex = new ConcurrentHashMap<>();

    @Override
    public Cart save(Cart cart) {
        storage.put(cart.getCartId(), cart);
        String indexKey = buildIndexKey(cart.getUserId(), cart.getProductOptionId());
        userOptionIndex.put(indexKey, cart.getCartId());
        return cart;
    }

    @Override
    public Optional<Cart> findById(Long cartId) {
        return Optional.ofNullable(storage.get(cartId));
    }

    @Override
    public List<Cart> findByUserId(Long userId) {
        return storage.values().stream()
                .filter(cart -> cart.getUserId().equals(userId))
                .collect(Collectors.toList());
    }

    @Override
    public Optional<Cart> findByUserIdAndProductOptionId(Long userId, Long productOptionId) {
        String indexKey = buildIndexKey(userId, productOptionId);
        Long cartId = userOptionIndex.get(indexKey);

        if (cartId == null) {
            return Optional.empty();
        }

        return Optional.ofNullable(storage.get(cartId));
    }

    @Override
    public void delete(Long cartId) {
        Cart cart = storage.remove(cartId);
        if (cart != null) {
            String indexKey = buildIndexKey(cart.getUserId(), cart.getProductOptionId());
            userOptionIndex.remove(indexKey);
        }
    }

    @Override
    public void deleteByUserIdAndProductId(Long userId, Long productId) {
        List<Long> cartIdsToDelete = storage.values().stream()
                .filter(cart -> cart.getUserId().equals(userId))
                .filter(cart -> cart.getProductOptionId().equals(productId)) // 실제로는 productId로 필터링하려면 Product 정보 필요
                .map(Cart::getCartId)
                .toList();

        cartIdsToDelete.forEach(this::delete);
    }

    private String buildIndexKey(Long userId, Long productOptionId) {
        return userId + ":" + productOptionId;
    }
}
