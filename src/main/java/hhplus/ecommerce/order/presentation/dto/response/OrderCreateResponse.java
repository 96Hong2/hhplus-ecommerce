package hhplus.ecommerce.order.presentation.dto.response;

import hhplus.ecommerce.order.domain.model.OrderStatus;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;

@Getter
@RequiredArgsConstructor
public class OrderCreateResponse {
    private final Long orderId;
    private final String orderNumber;
    private final OrderStatus orderStatus;
    private final Long totalAmount;
    private final Long discountAmount;
    private final Long finalAmount;
    private final Long usedPoints;
    private final LocalDateTime expiresAt;
}
