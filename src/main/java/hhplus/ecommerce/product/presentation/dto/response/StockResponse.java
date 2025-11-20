package hhplus.ecommerce.product.presentation.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.RequiredArgsConstructor;


@Getter
@RequiredArgsConstructor
public class StockResponse {
    private final Long productOptionId;
    private final int physicalQuantity;
    private final int reservedQuantity;
    private final int availableQuantity;
    @JsonProperty("is_sold_out")
    private final boolean isSoldOut ;
}
