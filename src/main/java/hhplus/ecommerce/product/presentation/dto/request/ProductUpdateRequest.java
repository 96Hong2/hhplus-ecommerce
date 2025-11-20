package hhplus.ecommerce.product.presentation.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@RequiredArgsConstructor
public class ProductUpdateRequest {

    private String productName;
    private String category;
    private String description;
    private String imageUrl;

    @DecimalMin(value = "0", message = "상품 가격은 0원 이상이어야 합니다.")
    private BigDecimal price;

    @Min(value = 0, message = "상품 재고는 0 이상이어야 합니다.")
    private int stockQuantity;

    @Min(value = 0, message = " 판매 횟수는 0 이상이어야 합니다.")
    private Long salesCount;

    @JsonProperty("is_exposed")
    private boolean isExposed;
}
