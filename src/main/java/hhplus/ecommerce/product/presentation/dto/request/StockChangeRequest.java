package hhplus.ecommerce.product.presentation.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@RequiredArgsConstructor
public class StockChangeRequest {

    @NotNull(message = "상품옵션ID는 필수값입니다.")
    private Long productOptionId;

    @NotNull(message = "재고 변경 양은 필수값입니다.")
    private Integer amount;

    private String description;
    private Long updatedBy;
}
