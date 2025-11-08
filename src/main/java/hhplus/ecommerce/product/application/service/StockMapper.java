package hhplus.ecommerce.product.application.service;

import hhplus.ecommerce.product.domain.model.StockHistory;
import hhplus.ecommerce.product.domain.model.StockReservation;
import hhplus.ecommerce.product.presentation.dto.response.StockHistoryResponse;
import hhplus.ecommerce.product.presentation.dto.response.StockReservationResponse;
import org.springframework.stereotype.Component;

@Component
public class StockMapper {
    public StockHistoryResponse toStockHistoryResponse(StockHistory stockHistory) {
        return new StockHistoryResponse(
                stockHistory.getStockHistoryId(),
                stockHistory.getProductOptionId(),
                stockHistory.getAmount(),
                stockHistory.getBalance(),
                stockHistory.getDescription(),
                stockHistory.getUpdatedBy(),
                stockHistory.getCreatedAt());
    }

    public StockReservationResponse toStockReservationResponse(StockReservation stockReservation) {
        return new StockReservationResponse(
            stockReservation.getStockReservationId(),
            stockReservation.getProductOptionId(),
            stockReservation.getOrderId(),
            stockReservation.getReservedQuantity(),
            stockReservation.getReservationStatus(),
            stockReservation.getReservedAt(),
            stockReservation.getExpiresAt(),
            stockReservation.getUpdatedAt()
        );
    }
}
