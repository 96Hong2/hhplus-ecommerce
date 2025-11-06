package hhplus.ecommerce.cart.presentation.dto.response;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;

@Getter
@RequiredArgsConstructor
public class CartItemResponse {
    private final Long cartId;
    private final Long productId;
    private final Long productOptionId;
    private final String productName;
    private final String optionName;
    private final BigDecimal optionPrice;
    private final Integer quantity;
    private final BigDecimal subtotal;
    private final Boolean isSoldOut;
}
