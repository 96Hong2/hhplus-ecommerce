package hhplus.ecommerce.unitTest.cart.service;

import hhplus.ecommerce.cart.application.service.CartService;
import hhplus.ecommerce.cart.domain.model.Cart;
import hhplus.ecommerce.cart.domain.repository.CartRepository;
import hhplus.ecommerce.common.domain.exception.CartException;
import hhplus.ecommerce.product.domain.model.ProductOption;
import hhplus.ecommerce.product.domain.repository.ProductOptionRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CartServiceTest {

    @Mock
    private CartRepository cartRepository;

    @Mock
    private ProductOptionRepository productOptionRepository;

    @InjectMocks
    private CartService cartService;

    @Test
    @DisplayName("장바구니에 상품을 추가할 수 있다")
    void addToCart() {
        ProductOption mockOption = ProductOption.create(1L, "기본 옵션", BigDecimal.ZERO, 100, true);

        when(productOptionRepository.findById(anyLong())).thenReturn(Optional.of(mockOption));
        when(cartRepository.findByUserIdAndProductOptionId(anyLong(), anyLong())).thenReturn(Optional.empty());
        when(cartRepository.save(any(Cart.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Cart result = cartService.addToCart(1L, 1L, 2);

        assertThat(result).isNotNull();
        assertThat(result.getQuantity()).isEqualTo(2);
        verify(cartRepository, times(1)).save(any(Cart.class));
    }

    @Test
    @DisplayName("이미 장바구니에 있는 상품은 수량을 합산한다")
    void addToCartDuplicate() {
        Cart existingCart = Cart.create(1L, 1L, 2);
        ProductOption mockOption = ProductOption.create(1L, "기본 옵션", BigDecimal.ZERO, 100, true);

        when(productOptionRepository.findById(anyLong())).thenReturn(Optional.of(mockOption));
        when(cartRepository.findByUserIdAndProductOptionId(anyLong(), anyLong()))
                .thenReturn(Optional.of(existingCart));
        when(cartRepository.save(any(Cart.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Cart result = cartService.addToCart(1L, 1L, 3);

        assertThat(result.getQuantity()).isEqualTo(5);
        verify(cartRepository, times(1)).save(any(Cart.class));
    }

    @Test
    @DisplayName("품절된 상품은 장바구니에 추가할 수 없다")
    void addToCartSoldOut() {
        ProductOption soldOutOption = ProductOption.create(1L, "품절 옵션", BigDecimal.ZERO, 0, true);

        when(productOptionRepository.findById(anyLong())).thenReturn(Optional.of(soldOutOption));

        assertThatThrownBy(() -> cartService.addToCart(1L, 1L, 1))
                .isInstanceOf(CartException.class);

        verify(cartRepository, never()).save(any());
    }

    @Test
    @DisplayName("장바구니 수량을 수정할 수 있다")
    void updateCartQuantity() {
        Cart cart = Cart.create(1L, 1L, 2);

        when(cartRepository.findById(anyLong())).thenReturn(Optional.of(cart));
        when(cartRepository.save(any(Cart.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Cart result = cartService.updateCartQuantity(1L, 5);

        assertThat(result.getQuantity()).isEqualTo(5);
        verify(cartRepository, times(1)).save(any(Cart.class));
    }

    @Test
    @DisplayName("장바구니 목록을 조회할 수 있다")
    void getCartItems() {
        Cart cart1 = Cart.create(1L, 1L, 2);
        Cart cart2 = Cart.create(1L, 2L, 3);

        when(cartRepository.findByUserId(anyLong()))
                .thenReturn(List.of(cart1, cart2));

        List<Cart> result = cartService.getCartItems(1L);

        assertThat(result).hasSize(2);
        verify(cartRepository, times(1)).findByUserId(1L);
    }

    @Test
    @DisplayName("장바구니 항목을 삭제할 수 있다")
    void removeFromCart() {
        Cart cart = Cart.create(1L, 1L, 2);

        when(cartRepository.findById(anyLong())).thenReturn(Optional.of(cart));
        doNothing().when(cartRepository).deleteById(anyLong());

        cartService.removeFromCart(1L);

        verify(cartRepository, times(1)).deleteById(1L);
    }
}
