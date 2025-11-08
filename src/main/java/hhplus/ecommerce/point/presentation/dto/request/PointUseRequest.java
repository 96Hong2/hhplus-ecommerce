package hhplus.ecommerce.point.presentation.dto.request;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class PointUseRequest {
    // @NotNull(message = "사용 금액은 필수입니다.")
    // @Min(value = 1, message = "사용 금액은 1원 이상이어야 합니다.")
    private BigDecimal amount;

    // @NotNull(message = "주문 ID는 필수입니다.")
    private Long orderId;

    private String description;
}
