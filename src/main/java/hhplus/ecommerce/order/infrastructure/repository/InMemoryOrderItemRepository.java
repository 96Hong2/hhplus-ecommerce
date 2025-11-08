package hhplus.ecommerce.order.infrastructure.repository;

import hhplus.ecommerce.order.domain.model.OrderItem;
import hhplus.ecommerce.order.domain.repository.OrderItemRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Repository
public class InMemoryOrderItemRepository implements OrderItemRepository {
    ConcurrentHashMap<Long, OrderItem> storage = new ConcurrentHashMap<>();

    @Override
    public OrderItem save(OrderItem orderItem) {
        storage.put(orderItem.getOrderItemId(), orderItem);
        return orderItem;
    }

    @Override
    public Optional<OrderItem> findById(Long orderItemId) {
        return Optional.ofNullable(storage.get(orderItemId));
    }

    @Override
    public List<OrderItem> findAllByOrderId(Long orderId) {
        return storage.values().stream()
                .filter(item -> item.getOrderId().equals(orderId))
                .collect(Collectors.toList());
    }

    @Override
    public List<OrderItem> saveAll(List<OrderItem> orderItems) {
        orderItems.forEach(item -> storage.put(item.getOrderItemId(), item));
        return orderItems;
    }
}
