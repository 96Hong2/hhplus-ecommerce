package hhplus.ecommerce.product.presentation.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@RequiredArgsConstructor
public class ProductRegistrationRequest {

    @NotNull(message = "상품명은 필수입니다.")
    private String productName;

    @NotNull(message = "카테고리는 필수입니다.")
    private String category;

    private String description;
    private String imageUrl;

    @NotNull(message = "상품 가격은 필수입니다.")
    @DecimalMin(value = "0", message = "상품 가격은 0원 이상이어야 합니다.")
    private BigDecimal price;

    @Min(value = 0, message = "상품 재고는 0 이상이어야 합니다.")
    private int stockQuantity;

    @JsonProperty("is_exposed")
    private boolean isExposed;
}
