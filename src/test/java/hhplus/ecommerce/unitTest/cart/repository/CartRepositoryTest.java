package hhplus.ecommerce.unitTest.cart.repository;

import hhplus.ecommerce.cart.domain.model.Cart;
import hhplus.ecommerce.cart.infrastructure.repository.InMemoryCartRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class CartRepositoryTest {

    private InMemoryCartRepository cartRepository;

    @BeforeEach
    void setUp() {
        cartRepository = new InMemoryCartRepository();
    }

    @Test
    @DisplayName("장바구니 항목을 저장할 수 있다")
    void saveCart() {
        Cart cart = Cart.create(1L, 1L, 2);

        Cart savedCart = cartRepository.save(cart);

        assertThat(savedCart).isNotNull();
        assertThat(savedCart.getCartId()).isNotNull();
        assertThat(savedCart.getQuantity()).isEqualTo(2);
    }

    @Test
    @DisplayName("ID로 장바구니 항목을 조회할 수 있다")
    void findById() {
        Cart cart = Cart.create(1L, 1L, 2);
        Cart savedCart = cartRepository.save(cart);

        Optional<Cart> foundCart = cartRepository.findById(savedCart.getCartId());

        assertThat(foundCart).isPresent();
        assertThat(foundCart.get().getQuantity()).isEqualTo(2);
    }

    @Test
    @DisplayName("사용자 ID로 장바구니 목록을 조회할 수 있다")
    void findByUserId() {
        cartRepository.save(Cart.create(1L, 1L, 2));
        cartRepository.save(Cart.create(1L, 2L, 3));
        cartRepository.save(Cart.create(2L, 3L, 1));

        List<Cart> userCarts = cartRepository.findByUserId(1L);

        assertThat(userCarts).hasSize(2);
    }

    @Test
    @DisplayName("사용자 ID와 상품 옵션 ID로 장바구니 항목을 조회할 수 있다")
    void findByUserIdAndProductOptionId() {
        Cart cart = Cart.create(1L, 1L, 2);
        cartRepository.save(cart);

        Optional<Cart> foundCart = cartRepository.findByUserIdAndProductOptionId(1L, 1L);

        assertThat(foundCart).isPresent();
        assertThat(foundCart.get().getQuantity()).isEqualTo(2);
    }

    @Test
    @DisplayName("장바구니 항목을 삭제할 수 있다")
    void deleteCart() {
        Cart cart = Cart.create(1L, 1L, 2);
        Cart savedCart = cartRepository.save(cart);

        cartRepository.delete(savedCart.getCartId());

        Optional<Cart> foundCart = cartRepository.findById(savedCart.getCartId());
        assertThat(foundCart).isEmpty();
    }

    @Test
    @DisplayName("사용자 ID와 상품 ID로 장바구니 항목을 삭제할 수 있다")
    void deleteByUserIdAndProductId() {
        cartRepository.save(Cart.create(1L, 1L, 2));
        cartRepository.save(Cart.create(1L, 2L, 3));

        cartRepository.deleteByUserIdAndProductId(1L, 1L);

        List<Cart> remainingCarts = cartRepository.findByUserId(1L);
        assertThat(remainingCarts).hasSize(1);
    }
}
