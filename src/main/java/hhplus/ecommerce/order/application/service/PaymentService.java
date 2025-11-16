package hhplus.ecommerce.order.application.service;

import hhplus.ecommerce.common.domain.exception.OrderException;
import hhplus.ecommerce.order.domain.model.Order;
import hhplus.ecommerce.order.domain.model.OrderItem;
import hhplus.ecommerce.order.domain.repository.OrderItemRepository;
import hhplus.ecommerce.order.domain.repository.OrderRepository;
import hhplus.ecommerce.order.presentation.dto.request.PaymentRequest;
import hhplus.ecommerce.order.presentation.dto.response.PaymentResponse;
import hhplus.ecommerce.order.domain.model.PaymentMethod;
import hhplus.ecommerce.point.application.service.PointService;
import hhplus.ecommerce.product.application.service.StockService;
import hhplus.ecommerce.product.domain.model.StockReservation;
import hhplus.ecommerce.product.domain.repository.StockReservationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final StockReservationRepository stockReservationRepository;
    private final StockService stockService;
    private final PointService pointService;

    /**
     * 주문 결제
     * @param orderId 주문 ID
     * @param request 결제 요청 (결제 수단)
     * @return 결제 완료 응답
     */
    @Transactional
    public PaymentResponse payOrder(Long orderId, PaymentRequest request) {
        // 1. 주문 조회 및 상태 확인
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> OrderException.orderNotFound(orderId));

        if (!order.canPayment()) {
            throw OrderException.paymentFailed("결제 가능한 상태가 아닙니다.");
        }

        // 2. 주문 항목 조회
        List<OrderItem> orderItems = orderItemRepository.findAllByOrderId(orderId);
        if (orderItems.isEmpty()) {
            throw OrderException.orderItemsEmpty();
        }

        // 3. 결제 수단 파싱 및 검증
        PaymentMethod method;
        try {
            method = PaymentMethod.valueOf(request.getPaymentMethod().toUpperCase());
        } catch (Exception e) {
            throw OrderException.invalidPaymentMethod(request.getPaymentMethod());
        }

        // 4. 재고 예약 확정 (각 주문 항목에 대해)
        for (OrderItem orderItem : orderItems) {
            StockReservation reservation = stockReservationRepository
                    .findByProductOptionIdAndOrderId(orderItem.getProductOptionId(), orderId)
                    .orElseThrow(() -> OrderException.paymentFailed(
                            "재고 예약을 찾을 수 없습니다. [상품옵션ID: " + orderItem.getProductOptionId() + "]"));

            stockService.confirmStockReservation(reservation.getStockReservationId());
        }

        // 5. 포인트 결제 처리 (필요 시)
        if (method == PaymentMethod.POINT) {
            BigDecimal amount = order.getFinalAmount();
            pointService.usePoint(order.getUserId(), amount, orderId, "주문 결제");
        }

        // 6. 주문 상태 업데이트 (PAID) 및 결제수단 반영
        Order paidOrder = order.payWithMethod(method);
        orderRepository.save(paidOrder);

        // 7. 결제 결과 반환
        return new PaymentResponse(
                paidOrder.getOrderId(),
                paidOrder.getOrderNumber(),
                paidOrder.getOrderStatus(),
                paidOrder.getFinalAmount(),
                method.name(),
                LocalDateTime.now()
        );
    }
}
