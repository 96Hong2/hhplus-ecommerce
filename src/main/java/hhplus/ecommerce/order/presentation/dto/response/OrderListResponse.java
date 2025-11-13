package hhplus.ecommerce.order.presentation.dto.response;
import hhplus.ecommerce.order.domain.model.OrderStatus;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@RequiredArgsConstructor
public class OrderListResponse {
    private final Long orderId;
    private final String orderNumber;
    private final OrderStatus orderStatus;
    private final BigDecimal finalAmount;
    private final LocalDateTime createdAt;
    private final LocalDateTime expiresAt;
}
