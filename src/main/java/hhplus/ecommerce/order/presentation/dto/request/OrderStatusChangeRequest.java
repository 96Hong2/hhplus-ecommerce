package hhplus.ecommerce.order.presentation.dto.request;

import hhplus.ecommerce.order.domain.model.OrderStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@RequiredArgsConstructor
public class OrderStatusChangeRequest {
    @NotNull(message = "주문 ID는 필수입니다.")
    private final Long orderId;

    @NotNull(message = "주문 상태는 필수입니다.")
    private final OrderStatus orderStatus;
}
