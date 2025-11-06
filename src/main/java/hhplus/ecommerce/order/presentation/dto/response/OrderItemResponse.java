package hhplus.ecommerce.order.presentation.dto.response;

import hhplus.ecommerce.order.domain.model.OrderItemStatus;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class OrderItemResponse {
    private final Long orderItemId;
    private final String productName;
    private final String optionName;
    private final Integer quantity;
    private final Long price;
    private final OrderItemStatus itemStatus;
}
