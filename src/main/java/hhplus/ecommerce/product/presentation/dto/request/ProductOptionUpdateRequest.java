package hhplus.ecommerce.product.presentation.dto.request;

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
    private boolean isExposed;
}
