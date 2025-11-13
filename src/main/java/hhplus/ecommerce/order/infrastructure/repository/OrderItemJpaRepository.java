package hhplus.ecommerce.order.infrastructure.repository;

import hhplus.ecommerce.order.domain.model.OrderItem;
import hhplus.ecommerce.order.domain.model.OrderItemStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface OrderItemJpaRepository extends JpaRepository<OrderItem, Long> {

    // 주문별 주문 상품 조회
    List<OrderItem> findByOrderId(Long orderId);

    // 주문별 + 상태별 주문 상품 조회
    @Query("SELECT oi FROM OrderItem oi WHERE oi.orderId = :orderId AND oi.itemStatus = :status")
    List<OrderItem> findByOrderIdAndStatus(@Param("orderId") Long orderId, @Param("status") OrderItemStatus status);

    // 상품 옵션별 주문 상품 조회
    @Query("SELECT oi FROM OrderItem oi WHERE oi.productOptionId = :productOptionId")
    List<OrderItem> findByProductOptionId(@Param("productOptionId") Long productOptionId);
}
