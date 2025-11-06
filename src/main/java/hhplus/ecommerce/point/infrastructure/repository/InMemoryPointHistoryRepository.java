package hhplus.ecommerce.point.infrastructure.repository;

import hhplus.ecommerce.point.domain.model.PointHistory;
import hhplus.ecommerce.point.domain.model.TransactionType;
import hhplus.ecommerce.point.domain.repository.PointHistoryRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Repository
public class InMemoryPointHistoryRepository implements PointHistoryRepository {

    // CopyOnWriteArrayList : 읽기 작업 시 락 없이 동시 접근이 가능하며, 쓰기 시에만 락 걸고 배열을 복사
    private final CopyOnWriteArrayList<PointHistory> pointHistoryStorage = new CopyOnWriteArrayList<>();
    private final AtomicLong sequence = new AtomicLong(1); // PointHistory 전용 시퀀스

    @Override
    public PointHistory save(PointHistory pointHistory) {
        Long id = pointHistory.getPointHistoryId();

        if (id == null) {
            id = sequence.getAndIncrement();
            PointHistory newHistory = new PointHistory(
                    id,
                    pointHistory.getUserId(),
                    pointHistory.getAmount(),
                    pointHistory.getBalanceAfter(),
                    pointHistory.getOrderId(),
                    pointHistory.getDescription()
            );
            pointHistoryStorage.add(newHistory);
            return newHistory;
        }

        pointHistoryStorage.add(pointHistory);
        return pointHistory;
    }

    @Override
    public List<PointHistory> findByUserId(Long userId) {
        return pointHistoryStorage.stream()
                .filter(pointHistory -> pointHistory.getUserId().equals(userId))
                .sorted((h1, h2) -> h2.getCreatedAt().compareTo(h1.getCreatedAt())) // 내림차순(최신순)
                .collect(Collectors.toList());
    }

    @Override
    public List<PointHistory> findByUserIdAndTransactionType(Long userId, TransactionType transactionType) {
        return pointHistoryStorage.stream()
                .filter(pointHistory -> pointHistory.getUserId().equals(userId))
                .filter(pointHistory -> pointHistory.getTransactionType() == transactionType)
                .sorted((h1, h2) -> h2.getCreatedAt().compareTo(h1.getCreatedAt()))
                .collect(Collectors.toList());
    }

    @Override
    public List<PointHistory> findByUserIdWithPaging(Long userId, int page, int size) {
        int offset = page * size;

        return pointHistoryStorage.stream()
                .filter(pointHistory -> pointHistory.getUserId().equals(userId))
                .sorted((h1, h2) -> h2.getCreatedAt().compareTo(h1.getCreatedAt()))
                .skip(offset)
                .limit(size)
                .collect(Collectors.toList());
    }

    @Override
    public List<PointHistory> findByUserIdAndTransactionTypeWithPaging(Long userId, TransactionType transactionType, int page, int size) {
        int offset = page * size;

        return pointHistoryStorage.stream()
                .filter(pointHistory -> (pointHistory.getUserId().equals(userId)))
                .filter(pointHistory -> (pointHistory.getTransactionType() == transactionType))
                .sorted((h1, h2) -> h2.getCreatedAt().compareTo(h1.getCreatedAt()))
                .skip(offset)
                .limit(size)
                .collect(Collectors.toList());
    }

    @Override
    public long countByUserId(Long userId) {
        return pointHistoryStorage.stream()
                .filter(pointHistory -> pointHistory.getUserId().equals(userId))
                .count();
    }

    @Override
    public long countByUserIdAndTransactionType(Long userId, TransactionType transactionType) {
        return pointHistoryStorage.stream()
                .filter(pointHistory -> pointHistory.getUserId().equals(userId))
                .filter(pointHistory -> (pointHistory.getTransactionType() == transactionType))
                .count();
    }
}
