package hhplus.ecommerce.cart.application.service;

import hhplus.ecommerce.cart.domain.model.Cart;
import hhplus.ecommerce.cart.presentation.dto.response.CartItemResponse;
import hhplus.ecommerce.cart.presentation.dto.response.CartResponse;
import hhplus.ecommerce.product.domain.model.Product;
import hhplus.ecommerce.product.domain.model.ProductOption;
import hhplus.ecommerce.product.domain.repository.ProductOptionRepository;
import hhplus.ecommerce.product.domain.repository.ProductRepository;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Cart 관련 Entity와 DTO 간 변환
 * 실시간 가격 및 재고 상태 반영
 */
@Component
public class CartMapper {

    private final ProductOptionRepository productOptionRepository;
    private final ProductRepository productRepository;

    public CartMapper(ProductOptionRepository productOptionRepository,
                      ProductRepository productRepository) {
        this.productOptionRepository = productOptionRepository;
        this.productRepository = productRepository;
    }

    public CartItemResponse toCartItemResponse(Cart cart) {
        ProductOption productOption = productOptionRepository.findById(cart.getProductOptionId())
                .orElseThrow(() -> new RuntimeException("상품 옵션을 찾을 수 없습니다."));

        Product product = productRepository.findById(productOption.getProductId())
                .orElseThrow(() -> new RuntimeException("상품을 찾을 수 없습니다."));

        BigDecimal subtotal = (product.getPrice().add(productOption.getPriceAdjustment())).multiply(BigDecimal.valueOf(cart.getQuantity()));

        return new CartItemResponse(
                cart.getCartId(),
                product.getProductId(),
                productOption.getProductOptionId(),
                product.getProductName(),
                productOption.getOptionName(),
                productOption.getPriceAdjustment(),
                cart.getQuantity(),
                subtotal,
                productOption.isSoldOut()
        );
    }

    public CartResponse toCartResponse(List<Cart> carts) {
        List<CartItemResponse> items = carts.stream()
                .map(this::toCartItemResponse)
                .collect(Collectors.toList());

        BigDecimal totalAmount = items.stream()
                .map(c -> c.getSubtotal())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new CartResponse(items, totalAmount);
    }
}

