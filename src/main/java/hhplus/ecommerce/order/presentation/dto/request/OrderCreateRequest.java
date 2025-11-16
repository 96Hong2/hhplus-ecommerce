package hhplus.ecommerce.order.presentation.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class OrderCreateRequest {
    @NotEmpty(message = "주문 아이템은 필수입니다")
    @Valid
    private List<OrderItemRequest> items;

    private Long couponId;
}
