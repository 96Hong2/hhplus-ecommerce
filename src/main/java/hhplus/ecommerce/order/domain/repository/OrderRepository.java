package hhplus.ecommerce.order.domain.repository;

import hhplus.ecommerce.order.domain.model.Order;
import hhplus.ecommerce.order.domain.model.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {

    Optional<Order> findByOrderNumber(String orderNumber);

    List<Order> findByUserId(Long userId);

    List<Order> findByUserIdAndOrderStatus(Long userId, OrderStatus status);

    // 만료된 주문 조회 (PENDING 상태이고 만료 시간이 지난 주문)
    @Query("SELECT o FROM Order o WHERE o.orderStatus = 'PENDING' AND o.expiresAt < :currentTime")
    List<Order> findExpiredOrders(@Param("currentTime") LocalDateTime currentTime);

    // 특정 상품 옵션에 대한 최근 주문 조회
    @Query("SELECT o FROM Order o JOIN OrderItem oi ON o.orderId = oi.orderId " +
           "WHERE oi.productOptionId = :productOptionId AND o.createdAt >= :startDate")
    List<Order> findRecentOrdersByProductOption(@Param("productOptionId") Long productOptionId,
                                                @Param("startDate") LocalDateTime startDate);
}
