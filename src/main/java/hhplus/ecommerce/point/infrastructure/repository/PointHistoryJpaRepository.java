package hhplus.ecommerce.point.infrastructure.repository;

import hhplus.ecommerce.point.domain.model.PointHistory;
import hhplus.ecommerce.point.domain.model.TransactionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PointHistoryJpaRepository extends JpaRepository<PointHistory, Long> {

    // 사용자별 포인트 이력 조회 (최신순)
    @Query("SELECT ph FROM PointHistory ph WHERE ph.userId = :userId ORDER BY ph.createdAt DESC")
    List<PointHistory> findByUserIdOrderByCreatedAtDesc(@Param("userId") Long userId);

    // 사용자별 + 거래 타입별 이력 조회
    @Query("SELECT ph FROM PointHistory ph WHERE ph.userId = :userId AND ph.transactionType = :type ORDER BY ph.createdAt DESC")
    List<PointHistory> findByUserIdAndType(@Param("userId") Long userId, @Param("type") TransactionType type);

    // 주문별 포인트 이력 조회
    @Query("SELECT ph FROM PointHistory ph WHERE ph.orderId = :orderId")
    List<PointHistory> findByOrderId(@Param("orderId") Long orderId);
}
