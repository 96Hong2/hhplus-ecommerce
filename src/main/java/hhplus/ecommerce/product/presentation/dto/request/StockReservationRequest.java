package hhplus.ecommerce.product.presentation.dto.request;

import hhplus.ecommerce.product.domain.model.ReservationStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@RequiredArgsConstructor
public class StockReservationRequest {

    @NotNull(message = "상품옵션ID는 필수입니다.")
    private Long productOptionId;

    @NotNull(message = "주문ID는 필수입니다.")
    private Long orderId;

    @NotNull(message = "예약할 재고량은 필수입니다.")
    @Positive
    private int reservedQuantity;

    private ReservationStatus reservationStatus;

    @PositiveOrZero
    private int expiryMinutes;
}
