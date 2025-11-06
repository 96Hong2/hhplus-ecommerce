package hhplus.ecommerce.product.infrastructure.repository;

import hhplus.ecommerce.product.domain.model.ReservationStatus;
import hhplus.ecommerce.product.domain.model.StockReservation;
import hhplus.ecommerce.product.domain.repository.StockReservationRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Repository
public class InMemoryStockReservationRepository implements StockReservationRepository {

    private final ConcurrentHashMap<Long, StockReservation> stockReservationMap = new ConcurrentHashMap<>();

    @Override
    public StockReservation save(StockReservation stockReservation) {
        if (stockReservation.getStockReservationId() == null) {
            StockReservation newStockReservation = stockReservation.getExpiresAt() == null
                    ? StockReservation.create(
                            stockReservation.getProductOptionId(),
                            stockReservation.getOrderId(),
                            stockReservation.getReservedQuantity())
                    : StockReservation.create(
                            stockReservation.getProductOptionId(),
                            stockReservation.getOrderId(),
                            stockReservation.getReservedQuantity(),
                            stockReservation.getExpiresAt());

            stockReservationMap.put(newStockReservation.getStockReservationId(), newStockReservation);
            return newStockReservation;
        }
        stockReservationMap.put(stockReservation.getStockReservationId(), stockReservation);
        return stockReservation;
    }

    @Override
    public Optional<StockReservation> findByStockReservationId(Long stockReservationId)
    {
        return Optional.ofNullable(stockReservationMap.get(stockReservationId));
    }

    @Override
    public Optional<StockReservation> findByProductOptionIdAndOrderId(Long productOptionId, Long orderId) {
        return stockReservationMap.values().stream()
                .filter(stockReservation -> stockReservation.getOrderId().equals(orderId))
                .filter(stockReservation -> stockReservation.getProductOptionId().equals(productOptionId))
                .findFirst();
    }

    @Override
    public List<StockReservation> findAllByProductOptionId(Long productOptionId) {
        return stockReservationMap.values().stream()
                .filter(stockReservation -> stockReservation.getProductOptionId().equals(productOptionId))
                .collect(Collectors.toList());
    }

    @Override
    public List<StockReservation> findAllByReservationStatus(ReservationStatus reservationStatus) {
        return stockReservationMap.values().stream()
                .filter(stockReservation -> stockReservation.getReservationStatus().equals(reservationStatus))
                .collect(Collectors.toList());
    }

    @Override
    public List<StockReservation> findAllReservedByProductOptionId(Long productOptionId) {
        return stockReservationMap.values().stream()
                .filter(stockReservation -> stockReservation.getProductOptionId().equals(productOptionId))
                .filter(stockReservation -> stockReservation.getReservationStatus().equals(ReservationStatus.RESERVED))
                .collect(Collectors.toList());
    }
}
