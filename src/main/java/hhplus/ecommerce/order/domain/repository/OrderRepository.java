package hhplus.ecommerce.order.domain.repository;

import hhplus.ecommerce.order.domain.model.Order;
import hhplus.ecommerce.order.domain.model.OrderStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface OrderRepository {

    public Order save(Order order);

    public Optional<Order> findById(Long orderId);

    Optional<Order> findByOrderNumber(String orderNumber);

    List<Order> findByUserId(Long userId);

    List<Order> findByUserIdAndStatus(Long userId, OrderStatus status);

    List<Order> findExpiredOrders(LocalDateTime currentTime);

    List<Order> findRecentOrdersByProductOption(Long productOptionId, int days);
}
