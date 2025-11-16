package hhplus.ecommerce.unitTest.cart.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import hhplus.ecommerce.cart.application.service.CartMapper;
import hhplus.ecommerce.cart.application.service.CartService;
import hhplus.ecommerce.cart.domain.model.Cart;
import hhplus.ecommerce.cart.presentation.controller.CartController;
import hhplus.ecommerce.cart.presentation.dto.request.CartAddRequest;
import hhplus.ecommerce.cart.presentation.dto.request.CartUpdateRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CartController.class)
class CartControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CartService cartService;

    @MockitoBean
    private CartMapper cartMapper;

    @Test
    @DisplayName("장바구니 추가 API 테스트")
    void addToCart() throws Exception {
        CartAddRequest request = new CartAddRequest();
        request.setProductOptionId(1L);
        request.setQuantity(2);

        Cart mockCart = Cart.create(1L, 1L, 2);

        when(cartService.addToCart(anyLong(), anyLong(), anyInt()))
                .thenReturn(mockCart);

        mockMvc.perform(post("/api/cart/{userId}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        verify(cartService, times(1)).addToCart(anyLong(), anyLong(), anyInt());
    }

    @Test
    @DisplayName("장바구니 조회 API 테스트")
    void getCart() throws Exception {
        when(cartService.getCartItems(anyLong()))
                .thenReturn(List.of());

        mockMvc.perform(get("/api/cart/{userId}", 1L))
                .andExpect(status().isOk());

        verify(cartService, times(1)).getCartItems(1L);
    }

    @Test
    @DisplayName("장바구니 수량 수정 API 테스트")
    void updateCartQuantity() throws Exception {
        CartUpdateRequest request = new CartUpdateRequest();
        request.setQuantity(5);

        Cart mockCart = Cart.create(1L, 1L, 5);

        when(cartService.updateCartQuantity(anyLong(), anyInt()))
                .thenReturn(mockCart);

        mockMvc.perform(put("/api/cart/{cartId}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        verify(cartService, times(1)).updateCartQuantity(1L, 5);
    }

    @Test
    @DisplayName("장바구니 항목 삭제 API 테스트")
    void removeFromCart() throws Exception {
        doNothing().when(cartService).removeFromCart(anyLong());

        mockMvc.perform(delete("/api/cart/{cartId}", 1L))
                .andExpect(status().isOk());

        verify(cartService, times(1)).removeFromCart(1L);
    }
}
