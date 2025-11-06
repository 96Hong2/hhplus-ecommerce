package hhplus.ecommerce.unitTest.cart.domain;

import hhplus.ecommerce.cart.domain.model.Cart;
import hhplus.ecommerce.common.domain.exception.CartException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class CartTest {

    @Test
    @DisplayName("장바구니 아이템을 정상적으로 생성할 수 있다.")
    void createCart() {
        // when
        Cart cart = Cart.create(1L, 1L, 2);

        // then
        assertThat(cart.getCartId()).isNotNull();
        assertThat(cart.getUserId()).isEqualTo(1L);
        assertThat(cart.getProductOptionId()).isEqualTo(1L);
        assertThat(cart.getQuantity()).isEqualTo(2);
        assertThat(cart.getCreatedAt()).isNotNull();
        assertThat(cart.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("장바구니 생성 시 userId가 null이면 예외가 발생한다.")
    void createCartWithNullUserId() {
        // when & then
        assertThatThrownBy(() -> Cart.create(null, 1L, 2))
                .isInstanceOf(CartException.class)
                .hasMessageContaining("사용자 ID는 필수입니다.");
    }

    @Test
    @DisplayName("장바구니 생성 시 productOptionId가 null이면 예외가 발생한다.")
    void createCartWithNullProductOptionId() {
        // when & then
        assertThatThrownBy(() -> Cart.create(1L, null, 2))
                .isInstanceOf(CartException.class)
                .hasMessageContaining("상품 옵션 ID는 필수입니다.");
    }

    @Test
    @DisplayName("장바구니 생성 시 수량이 0 이하면 예외가 발생한다.")
    void createCartWithInvalidQuantity() {
        // when & then
        assertThatThrownBy(() -> Cart.create(1L, 1L, 0))
                .isInstanceOf(CartException.class)
                .hasMessageContaining("유효하지 않은 수량입니다.");

        assertThatThrownBy(() -> Cart.create(1L, 1L, -1))
                .isInstanceOf(CartException.class)
                .hasMessageContaining("유효하지 않은 수량입니다.");
    }

    @Test
    @DisplayName("장바구니 수량을 정상적으로 변경할 수 있다.")
    void updateQuantity() {
        // given
        Cart cart = Cart.create(1L, 1L, 2);

        // when
        cart.updateQuantity(5);

        // then
        assertThat(cart.getQuantity()).isEqualTo(5);
    }

    @Test
    @DisplayName("장바구니 수량 변경 시 0 이하면 예외가 발생한다.")
    void updateQuantityWithInvalidQuantity() {
        // given
        Cart cart = Cart.create(1L, 1L, 2);

        // when & then
        assertThatThrownBy(() -> cart.updateQuantity(0))
                .isInstanceOf(CartException.class)
                .hasMessageContaining("유효하지 않은 수량입니다.");
    }

    @Test
    @DisplayName("장바구니 수량을 추가할 수 있다.")
    void addQuantity() {
        // given
        Cart cart = Cart.create(1L, 1L, 2);

        // when
        cart.addQuantity(3);

        // then
        assertThat(cart.getQuantity()).isEqualTo(5);
    }

    @Test
    @DisplayName("장바구니 수량 추가 시 0 이하면 예외가 발생한다.")
    void addQuantityWithInvalidQuantity() {
        // given
        Cart cart = Cart.create(1L, 1L, 2);

        // when & then
        assertThatThrownBy(() -> cart.addQuantity(0))
                .isInstanceOf(CartException.class)
                .hasMessageContaining("유효하지 않은 수량입니다.");

        assertThatThrownBy(() -> cart.addQuantity(-1))
                .isInstanceOf(CartException.class)
                .hasMessageContaining("유효하지 않은 수량입니다.");
    }
}
