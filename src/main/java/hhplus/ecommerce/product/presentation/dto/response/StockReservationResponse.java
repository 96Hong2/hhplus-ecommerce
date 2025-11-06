package hhplus.ecommerce.product.presentation.dto.response;

import hhplus.ecommerce.product.domain.model.ReservationStatus;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;

@Getter
@RequiredArgsConstructor
public class StockReservationResponse {
    private final Long stockReservationId;
    private final Long productOptionId;
    private final Long orderId;
    private final int reservedQuantity;
    private final ReservationStatus reservationStatus;
    private final LocalDateTime reservedAt;
    private final LocalDateTime expiresAt;
    private final LocalDateTime updatedAt;
}
