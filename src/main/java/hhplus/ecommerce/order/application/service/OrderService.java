package hhplus.ecommerce.order.application.service;

import hhplus.ecommerce.common.domain.exception.OrderException;
import hhplus.ecommerce.coupon.application.service.CouponService;
import hhplus.ecommerce.coupon.domain.model.Coupon;
import hhplus.ecommerce.order.application.dto.OrderDetailInfo;
import hhplus.ecommerce.order.application.dto.OrderItemDetailInfo;
import hhplus.ecommerce.order.application.dto.OrderItemInfo;
import hhplus.ecommerce.order.domain.model.Order;
import hhplus.ecommerce.order.domain.model.OrderItem;
import hhplus.ecommerce.order.domain.repository.OrderItemRepository;
import hhplus.ecommerce.order.domain.repository.OrderRepository;
import hhplus.ecommerce.order.presentation.dto.request.OrderItemRequest;
import hhplus.ecommerce.product.application.service.ProductService;
import hhplus.ecommerce.product.application.service.StockService;
import hhplus.ecommerce.product.domain.model.ProductOption;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ProductService productService;
    private final StockService stockService;
    private final CouponService couponService;

    /**
     * 상품 옵션을 조회하고 최종 단가를 계산
     * @param productOptionId 상품 옵션 ID
     * @return OrderItemInfo (상품 정보와 단가 포함)
     */
    public OrderItemInfo getProductOptionWithPrice(Long productOptionId, int quantity) {
        // 상품 옵션 조회
        List<ProductOption> productOptions = productService.getProductOptions(productOptionId);
        ProductOption productOption = productOptions.stream()
                .filter(po -> po.getProductOptionId().equals(productOptionId))
                .findFirst()
                .orElseThrow(() -> OrderException.orderCreationFailed(
                        "상품 옵션을 찾을 수 없습니다. ID: " + productOptionId));

        // 상품 정보 조회
        var productDetail = productService.getProductDetail(productOption.getProductId());

        // 최종 단가 계산 (기본 가격 + 옵션 가격 조정값)
        BigDecimal unitPrice = calculateUnitPrice(
                productDetail.getPrice(),
                productOption.getPriceAdjustment()
        );

        // subtotal은 도메인에서 계산하도록 OrderItem.calculateSubtotal 활용
        BigDecimal subtotal = unitPrice.multiply(BigDecimal.valueOf(quantity));

        return new OrderItemInfo(
                productOption.getProductId(),
                productOptionId,
                productDetail.getProductName(),
                productOption.getOptionName(),
                quantity,
                unitPrice,
                subtotal
        );
    }

    /**
     * 상품의 최종 단가 계산 (기본가 + 옵션 조정값)
     */
    private BigDecimal calculateUnitPrice(BigDecimal basePrice, BigDecimal priceAdjustment) {
        BigDecimal adjustment = priceAdjustment != null ? priceAdjustment : BigDecimal.ZERO;
        return basePrice.add(adjustment);
    }

    /**
     * 여러 주문 요청 아이템으로부터 주문 아이템 정보를 수집하고 총 금액 계산
     * @param itemRequests 주문 아이템 요청 목록
     * @return 주문 아이템 정보 목록
     */
    public List<OrderItemInfo> collectOrderItems(List<OrderItemRequest> itemRequests) {
        if (itemRequests == null || itemRequests.isEmpty()) {
            throw OrderException.orderItemsEmpty();
        }

        List<OrderItemInfo> orderItemInfos = new ArrayList<>();
        for (OrderItemRequest itemRequest : itemRequests) {
            OrderItemInfo itemInfo = getProductOptionWithPrice(
                    itemRequest.getProductOptionId(),
                    itemRequest.getQuantity()
            );
            orderItemInfos.add(itemInfo);
        }

        return orderItemInfos;
    }

    /**
     * 주문 아이템 목록으로부터 총 금액 계산
     */
    public BigDecimal calculateTotalAmount(List<OrderItemInfo> orderItems) {
        return orderItems.stream()
                .map(OrderItemInfo::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * 쿠폰 할인 금액 계산
     * @param couponId 쿠폰 ID (null 가능)
     * @param totalAmount 총 주문 금액
     * @return 할인 금액
     */
    public BigDecimal calculateCouponDiscount(Long couponId, BigDecimal totalAmount) {
        if (couponId == null) {
            return BigDecimal.ZERO;
        }

        Coupon coupon = couponService.getCouponById(couponId);
        if (!couponService.isCouponAvailable(coupon, totalAmount, LocalDateTime.now())) {
            throw OrderException.orderCreationFailed("사용할 수 없는 쿠폰입니다.");
        }

        Long discountLong = couponService.calculateDiscountAmount(coupon, totalAmount);
        return BigDecimal.valueOf(discountLong);
    }

    /**
     * 주문 아이템들의 재고를 예약
     */
    public void reserveStocks(Long orderId, List<OrderItemInfo> orderItems) {
        for (OrderItemInfo itemInfo : orderItems) {
            stockService.reserveStock(
                    orderId,
                    itemInfo.getProductOptionId(),
                    itemInfo.getQuantity()
            );
        }
    }

    /**
     * 주문 아이템들을 저장
     */
    public List<OrderItem> saveOrderItems(Long orderId, List<OrderItemInfo> orderItemInfos) {
        List<OrderItem> orderItems = new ArrayList<>();
        for (OrderItemInfo info : orderItemInfos) {
            OrderItem orderItem = OrderItem.create(
                    orderId,
                    info.getProductId(),
                    info.getProductOptionId(),
                    info.getProductName(),
                    info.getOptionName(),
                    info.getUnitPrice(),
                    info.getQuantity()
            );
            orderItems.add(orderItem);
        }

        return orderItemRepository.saveAll(orderItems);
    }

    /**
     * 주문 조회 (예외 처리 포함)
     */
    public Order getOrder(Long orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> OrderException.orderNotFound(orderId));
    }

    /**
     * 주문의 모든 아이템 조회
     */
    public List<OrderItem> getOrderItems(Long orderId) {
        return orderItemRepository.findAllByOrderId(orderId);
    }

    /**
     * Order와 OrderItem들을 OrderDetailInfo로 변환
     */
    public OrderDetailInfo toOrderDetailInfo(Order order, List<OrderItem> orderItems) {
        List<OrderItemDetailInfo> itemInfos = orderItems.stream()
                .map(this::toOrderItemDetailInfo)
                .collect(Collectors.toList());

        return new OrderDetailInfo(
                order.getOrderId(),
                order.getOrderNumber(),
                order.getOrderStatus(),
                order.getTotalAmount(),
                order.getDiscountAmount(),
                order.getFinalAmount(),
                order.getUsedPoints(),
                itemInfos,
                order.getCreatedAt(),
                order.getExpiresAt()
        );
    }

    /**
     * OrderItem을 OrderItemDetailInfo로 변환
     */
    public OrderItemDetailInfo toOrderItemDetailInfo(OrderItem orderItem) {
        return new OrderItemDetailInfo(
                orderItem.getOrderItemId(),
                orderItem.getProductName(),
                orderItem.getOptionName(),
                orderItem.getQuantity(),
                orderItem.getUnitPrice(),
                orderItem.getItemStatus()
        );
    }

    /**
     * 주문 번호 생성 (ORD + timestamp + userId)
     */
    public String generateOrderNumber(Long userId) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        return String.format("ORD%s%d", timestamp, userId);
    }

    /**
     * 주문 저장
     */
    public Order saveOrder(Order order) {
        return orderRepository.save(order);
    }

    /**
     * 주문 아이템 저장
     */
    public OrderItem saveOrderItem(OrderItem orderItem) {
        return orderItemRepository.save(orderItem);
    }

    /**
     * 주문 아이템 조회 (예외 처리 포함)
     */
    public OrderItem getOrderItem(Long orderItemId) {
        return orderItemRepository.findById(orderItemId)
                .orElseThrow(() -> OrderException.orderItemNotFound(orderItemId));
    }

    /**
     * 사용자의 주문 목록 조회 (상태 필터링 포함)
     */
    public List<Order> getOrdersByUserId(Long userId, hhplus.ecommerce.order.domain.model.OrderStatus status) {
        if (status != null) {
            return orderRepository.findByUserIdAndStatus(userId, status);
        }
        return orderRepository.findByUserId(userId);
    }
}

