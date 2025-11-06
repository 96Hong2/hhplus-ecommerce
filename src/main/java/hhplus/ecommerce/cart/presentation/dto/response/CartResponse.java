package hhplus.ecommerce.cart.presentation.dto.response;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Getter
@RequiredArgsConstructor
public class CartResponse {
    private final List<CartItemResponse> items;
    private final BigDecimal totalAmount;
}
