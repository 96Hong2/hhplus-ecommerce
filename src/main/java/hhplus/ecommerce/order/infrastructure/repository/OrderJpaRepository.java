package hhplus.ecommerce.order.infrastructure.repository;

import hhplus.ecommerce.order.domain.model.Order;
import hhplus.ecommerce.order.domain.model.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface OrderJpaRepository extends JpaRepository<Order, Long> {

    // 주문번호로 조회
    Optional<Order> findByOrderNumber(String orderNumber);

    // 사용자별 주문 조회 (최신순)
    @Query("SELECT o FROM Order o WHERE o.userId = :userId ORDER BY o.createdAt DESC")
    List<Order> findByUserIdOrderByCreatedAtDesc(@Param("userId") Long userId);

    // 사용자별 + 상태별 주문 조회
    @Query("SELECT o FROM Order o WHERE o.userId = :userId AND o.orderStatus = :status ORDER BY o.createdAt DESC")
    List<Order> findByUserIdAndStatus(@Param("userId") Long userId, @Param("status") OrderStatus status);

    // 만료된 PENDING 주문 조회 (타임아웃 처리용)
    @Query("SELECT o FROM Order o WHERE o.orderStatus = 'PENDING' AND o.expiresAt < :currentTime")
    List<Order> findExpiredPendingOrders(@Param("currentTime") LocalDateTime currentTime);

    // 특정 상품 옵션의 최근 주문 조회 (인기 상품 통계용)
    @Query("SELECT o FROM Order o JOIN OrderItem oi ON o.orderId = oi.orderId " +
           "WHERE oi.productOptionId = :productOptionId " +
           "AND o.orderStatus = 'PAID' " +
           "AND o.createdAt >= :startDate")
    List<Order> findRecentOrdersByProductOption(@Param("productOptionId") Long productOptionId,
                                                 @Param("startDate") LocalDateTime startDate);
}
