package hhplus.ecommerce.domain.repository;

import hhplus.ecommerce.domain.model.point.TransactionType;
import hhplus.ecommerce.domain.model.user.PointHistory;
import java.util.List;

public interface PointHistoryRepository {
    PointHistory save(PointHistory pointHistory);
    List<PointHistory> findByUserId(Long userId);
    List<PointHistory> findByUserIdAndTransactionType(Long userId, TransactionType transactionType);
    List<PointHistory> findByUserIdWithPaging(Long userId, int page, int size);
    List<PointHistory> findByUserIdAndTransactionTypeWithPaging(Long userId, TransactionType transactionType, int page, int size);
    int countByUserId(Long userId);
    int countByUserIdAndTransactionType(Long userId, TransactionType transactionType);
}

