package hhplus.ecommerce.product.presentation.dto.response;

import lombok.Getter;
import lombok.RequiredArgsConstructor;


@Getter
@RequiredArgsConstructor
public class StockResponse {
    private final Long productOptionId;
    private final int physicalQuantity;
    private final int reservedQuantity;
    private final int availableQuantity;
    private final boolean isSoldOut ;
}
