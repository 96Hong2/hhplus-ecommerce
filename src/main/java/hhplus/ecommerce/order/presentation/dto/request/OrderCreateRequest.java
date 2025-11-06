package hhplus.ecommerce.order.presentation.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
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

    @Min(value = 0, message = "사용 포인트는 0 이상이어야 합니다")
    private Long usedPoints = 0L;

    private Long couponId;
}
