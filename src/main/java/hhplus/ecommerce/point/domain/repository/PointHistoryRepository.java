package hhplus.ecommerce.point.domain.repository;

import hhplus.ecommerce.point.domain.model.PointHistory;
import hhplus.ecommerce.point.domain.model.TransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PointHistoryRepository extends JpaRepository<PointHistory, Long> {

    @Query("SELECT ph FROM PointHistory ph WHERE ph.userId = :userId ORDER BY ph.createdAt DESC")
    List<PointHistory> findByUserIdOrderByCreatedAtDesc(@Param("userId") Long userId);

    @Query("SELECT ph FROM PointHistory ph WHERE ph.userId = :userId AND ph.transactionType = :type ORDER BY ph.createdAt DESC")
    List<PointHistory> findByUserIdAndType(@Param("userId") Long userId, @Param("type") TransactionType type);

    @Query("SELECT ph FROM PointHistory ph WHERE ph.orderId = :orderId")
    List<PointHistory> findByOrderId(@Param("orderId") Long orderId);

    Page<PointHistory> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    Page<PointHistory> findByUserIdAndTransactionTypeOrderByCreatedAtDesc(Long userId, TransactionType transactionType, Pageable pageable);

    long countByUserId(Long userId);

    long countByUserIdAndTransactionType(Long userId, TransactionType transactionType);
}

