package hhplus.ecommerce.cart.presentation.controller;

import hhplus.ecommerce.cart.application.service.CartMapper;
import hhplus.ecommerce.cart.application.service.CartService;
import hhplus.ecommerce.cart.domain.model.Cart;
import hhplus.ecommerce.cart.presentation.dto.request.CartAddRequest;
import hhplus.ecommerce.cart.presentation.dto.response.CartItemResponse;
import hhplus.ecommerce.cart.presentation.dto.response.CartResponse;
import hhplus.ecommerce.cart.presentation.dto.request.CartUpdateRequest;
import hhplus.ecommerce.common.presentation.response.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
@RestController
@RequestMapping("/api/cart")
public class CartController {
    private final CartService cartService;
    private final CartMapper cartMapper;

    public CartController(CartService cartService, CartMapper cartMapper) {
        this.cartService = cartService;
        this.cartMapper = cartMapper;
    }

    /**
     * 장바구니 조회
     * GET /api/cart/{userId}
     */
    @GetMapping("/{userId}")
    public ApiResponse<CartResponse> getCart(@PathVariable Long userId) {
        List<Cart> carts = cartService.getCartItems(userId);
        CartResponse response = cartMapper.toCartResponse(carts);
        return ApiResponse.success(response);
    }

    /**
     * 장바구니 추가
     * POST /api/cart/{userId}
     */
    @PostMapping("/{userId}")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<CartItemResponse> addToCart(
            @PathVariable Long userId,
            @Valid @RequestBody CartAddRequest request) {

        Cart cart = cartService.addToCart(userId, request.getProductOptionId(), request.getQuantity());
        CartItemResponse response = cartMapper.toCartItemResponse(cart);

        return ApiResponse.success(response, "장바구니에 추가되었습니다.");
    }

    /**
     * 장바구니 수정
     * PATCH /api/cart/{cartId}
     */
    @PatchMapping("/{cartId}")
    public ApiResponse<CartItemResponse> updateCart(
            @PathVariable Long cartId,
            @Valid @RequestBody CartUpdateRequest request) {

        Cart cart = cartService.updateCartQuantity(cartId, request.getQuantity());
        CartItemResponse response = cartMapper.toCartItemResponse(cart);

        return ApiResponse.success(response, "장바구니가 수정되었습니다.");
    }

    /**
     * 장바구니 항목 삭제
     * DELETE /api/cart/{userId}/{productId}
     */
    @DeleteMapping("/{userId}/{productId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public ApiResponse<Void> removeFromCart(
            @PathVariable Long userId,
            @PathVariable Long productId) {

        cartService.removeByUserIdAndProductId(userId, productId);
        return ApiResponse.success(null, "장바구니 항목이 삭제되었습니다.");
    }
}
