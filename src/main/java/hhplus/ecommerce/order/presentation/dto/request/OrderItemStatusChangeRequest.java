package hhplus.ecommerce.order.presentation.dto.request;

import hhplus.ecommerce.order.domain.model.OrderItemStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Getter
@Setter
public class OrderItemStatusChangeRequest {
    @NotNull(message = "주문 항목 상태는 필수입니다.")
    private OrderItemStatus status;
}
