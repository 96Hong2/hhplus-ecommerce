package hhplus.ecommerce.point.presentation.dto.request;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class PointChargeRequest {

    // @NotNull(message = "충전 금액은 필수입니다.")
    // @Min(value = 1000, message = "최소 충전 금액은 1000원입니다.:")
    private BigDecimal amount;

    private String description;
}
