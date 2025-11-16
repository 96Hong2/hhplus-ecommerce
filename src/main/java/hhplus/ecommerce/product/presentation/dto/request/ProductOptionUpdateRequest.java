package hhplus.ecommerce.product.presentation.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@RequiredArgsConstructor
public class ProductOptionUpdateRequest {

    private String optionName;
    private BigDecimal priceAdjustment;
    @JsonProperty("is_exposed")
    private boolean isExposed;
}
