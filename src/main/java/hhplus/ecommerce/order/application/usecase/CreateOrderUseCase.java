package hhplus.ecommerce.order.application.usecase;

import hhplus.ecommerce.common.domain.exception.OrderException;
import hhplus.ecommerce.coupon.application.service.CouponService;
import hhplus.ecommerce.coupon.domain.model.Coupon;
import hhplus.ecommerce.order.domain.model.Order;
import hhplus.ecommerce.order.domain.model.OrderItem;
import hhplus.ecommerce.order.domain.repository.OrderItemRepository;
import hhplus.ecommerce.order.domain.repository.OrderRepository;
import hhplus.ecommerce.order.presentation.dto.request.OrderCreateRequest;
import hhplus.ecommerce.order.presentation.dto.request.OrderItemRequest;
import hhplus.ecommerce.order.presentation.dto.response.OrderCreateResponse;
import hhplus.ecommerce.product.application.service.ProductService;
import hhplus.ecommerce.product.application.service.StockService;
import hhplus.ecommerce.product.domain.model.ProductOption;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class CreateOrderUseCase {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ProductService productService;
    private final StockService stockService;
    private final CouponService couponService;

    /**
     * 주문 생성 UseCase
     * 1. 상품 옵션 검증 및 가격 계산
     * 2. 재고 예약
     * 3. 쿠폰 할인 계산
     * 4. 주문 생성
     * 5. 주문 아이템 생성
     */
    public OrderCreateResponse execute(Long userId, OrderCreateRequest request) {
        // 주문 아이템이 비어있는지 검증
        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw OrderException.orderItemsEmpty();
        }

        // 총 주문 금액 계산 및 주문 아이템 정보 수집
        BigDecimal totalAmount = BigDecimal.ZERO;
        List<OrderItemInfo> orderItemInfos = new ArrayList<>();

        for (OrderItemRequest itemRequest : request.getItems()) {
            // 상품 옵션 조회
            List<ProductOption> productOptions = productService.getProductOptions(itemRequest.getProductOptionId());
            ProductOption productOption = productOptions.stream()
                    .filter(po -> po.getProductOptionId().equals(itemRequest.getProductOptionId()))
                    .findFirst()
                    .orElseThrow(() -> OrderException.orderCreationFailed(
                            "상품 옵션을 찾을 수 없습니다. ID: " + itemRequest.getProductOptionId()));

            // 상품 정보 조회 (상품명을 얻기 위해)
            var productDetail = productService.getProductDetail(productOption.getProductId());

            // 최종 가격 계산 (기본 가격 + 옵션 가격 조정값)
            BigDecimal itemPrice = productDetail.getPrice().add(
                productOption.getPriceAdjustment() != null ? productOption.getPriceAdjustment() : BigDecimal.ZERO
            );
            BigDecimal subtotal = itemPrice.multiply(BigDecimal.valueOf(itemRequest.getQuantity()));
            totalAmount = totalAmount.add(subtotal);

            orderItemInfos.add(new OrderItemInfo(
                    productDetail.getProductName(),
                    productOption,
                    itemRequest.getQuantity(),
                    itemPrice,
                    subtotal
            ));
        }

        // 쿠폰 할인 금액 계산
        BigDecimal discountAmount = BigDecimal.ZERO;
        if (request.getCouponId() != null) {
            Coupon coupon = couponService.getCouponById(request.getCouponId());
            if (!couponService.isCouponAvailable(coupon, totalAmount, LocalDateTime.now())) {
                throw OrderException.orderCreationFailed("사용할 수 없는 쿠폰입니다.");
            }
            Long discountLong = couponService.calculateDiscountAmount(coupon, totalAmount);
            discountAmount = BigDecimal.valueOf(discountLong);
        }

        // 주문 번호 생성 (ORD + timestamp + userId)
        String orderNumber = generateOrderNumber(userId);

        // 주문 생성
        BigDecimal usedPoints = request.getUsedPoints() != null
            ? BigDecimal.valueOf(request.getUsedPoints())
            : BigDecimal.ZERO;

        Order order = Order.create(
                orderNumber,
                userId,
                totalAmount,
                discountAmount,
                usedPoints,
                request.getCouponId()
        );

        Order savedOrder = orderRepository.save(order);

        // 주문 아이템 생성 및 재고 예약
        List<OrderItem> orderItems = new ArrayList<>();
        for (OrderItemInfo info : orderItemInfos) {
            // 재고 예약
            stockService.reserveStock(
                    savedOrder.getOrderId(),
                    info.productOption.getProductOptionId(),
                    info.quantity
            );

            // 주문 아이템 생성
            OrderItem orderItem = new OrderItem(
                    savedOrder.getOrderId(),
                    info.productOption.getProductId(),
                    info.productOption.getProductOptionId(),
                    info.productName,
                    info.productOption.getOptionName(),
                    info.itemPrice,
                    info.quantity
            );
            orderItems.add(orderItem);
        }

        orderItemRepository.saveAll(orderItems);

        return new OrderCreateResponse(
                savedOrder.getOrderId(),
                savedOrder.getOrderNumber(),
                savedOrder.getOrderStatus(),
                savedOrder.getTotalAmount().longValue(),
                savedOrder.getDiscountAmount().longValue(),
                savedOrder.getFinalAmount().longValue(),
                savedOrder.getUsedPoints().longValue(),
                savedOrder.getExpiresAt()
        );
    }

    private String generateOrderNumber(Long userId) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        return String.format("ORD%s%d", timestamp, userId);
    }

    /**
     * 주문 아이템 정보를 담는 내부 클래스
     */
    private static class OrderItemInfo {
        String productName;
        ProductOption productOption;
        int quantity;
        BigDecimal itemPrice;
        BigDecimal subtotal;

        OrderItemInfo(String productName, ProductOption productOption, int quantity, BigDecimal itemPrice, BigDecimal subtotal) {
            this.productName = productName;
            this.productOption = productOption;
            this.quantity = quantity;
            this.itemPrice = itemPrice;
            this.subtotal = subtotal;
        }
    }
}
