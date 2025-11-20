package hhplus.ecommerce.order.presentation.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PaymentRequest {
    @NotBlank(message = "결제 수단은 필수입니다")
    private String paymentMethod;
}
