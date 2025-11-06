package hhplus.ecommerce.order.presentation.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PaymentRequest {
    @NotBlank(message = "결제 수단은 필수입니다")
    private String paymentMethod;

    @Min(value = 0, message = "사용 포인트는 0 이상이어야 합니다")
    private Long usedPoints = 0L;
}
