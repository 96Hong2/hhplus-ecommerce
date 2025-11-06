package hhplus.ecommerce.order.presentation.dto.response;

import hhplus.ecommerce.order.domain.model.OrderStatus;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;

@Getter
@RequiredArgsConstructor
public class PaymentResponse {
    private final Long orderId;
    private final String orderNumber;
    private final OrderStatus orderStatus;
    private final Long finalAmount;
    private final String paymentMethod;
    private final LocalDateTime paidAt;
}
