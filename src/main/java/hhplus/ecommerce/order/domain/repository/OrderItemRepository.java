package hhplus.ecommerce.order.domain.repository;

import hhplus.ecommerce.order.domain.model.OrderItem;

import java.util.List;
import java.util.Optional;

public interface OrderItemRepository {

    OrderItem save(OrderItem orderItem);

    Optional<OrderItem> findById(Long orderItemId);

    List<OrderItem> findAllByOrderId(Long orderId);

    List<OrderItem> saveAll(List<OrderItem> orderItems);
}
