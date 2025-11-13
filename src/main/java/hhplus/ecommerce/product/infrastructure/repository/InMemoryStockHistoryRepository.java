package hhplus.ecommerce.product.infrastructure.repository;

import hhplus.ecommerce.product.domain.model.StockHistory;
import hhplus.ecommerce.product.domain.repository.StockHistoryRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Repository
public class InMemoryStockHistoryRepository implements StockHistoryRepository {

    private final CopyOnWriteArrayList<StockHistory> stockHistoryList = new CopyOnWriteArrayList<>();

    @Override
    public StockHistory save(StockHistory stockHistory) {
        if (stockHistory.getStockHistoryId() == null) {
            StockHistory newStockHistory = StockHistory.create(
                    stockHistory.getProductOptionId(),
                    stockHistory.getAmount(),
                    stockHistory.getAdjustmentType(),
                    stockHistory.getBalance(),
                    stockHistory.getDescription(),
                    stockHistory.getUpdatedBy()
            );
            stockHistoryList.add(newStockHistory);
            return newStockHistory;
        }

        stockHistoryList.add(stockHistory);
        return stockHistory;
    }

    @Override
    public List<StockHistory> findAllByProductOptionIdWithPaging(Long productOptionId, int page, int size) {
        return stockHistoryList.stream()
                .filter(stockHistory -> stockHistory.getProductOptionId().equals(productOptionId))
                .sorted((h1, h2) -> h2.getCreatedAt().compareTo(h1.getCreatedAt()))
                .skip((long) page * size)
                .limit(size)
                .toList();
    }
}
