package hhplus.ecommerce.order.domain.repository;

import hhplus.ecommerce.order.domain.model.OrderItem;
import hhplus.ecommerce.order.domain.model.OrderItemStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    List<OrderItem> findByOrderId(Long orderId);

    @Query("SELECT oi FROM OrderItem oi WHERE oi.orderId = :orderId AND oi.itemStatus = :status")
    List<OrderItem> findByOrderIdAndStatus(@Param("orderId") Long orderId, @Param("status") OrderItemStatus status);

    @Query("SELECT oi FROM OrderItem oi WHERE oi.productOptionId = :productOptionId")
    List<OrderItem> findByProductOptionId(@Param("productOptionId") Long productOptionId);
}
