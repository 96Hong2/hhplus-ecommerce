package hhplus.ecommerce.order.infrastructure.repository;

import hhplus.ecommerce.order.domain.model.Order;
import hhplus.ecommerce.order.domain.model.OrderStatus;
import hhplus.ecommerce.order.domain.repository.OrderRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Repository
public class InMemoryOrderRepository implements OrderRepository {

    private final ConcurrentHashMap<Long, Order> storage = new ConcurrentHashMap<>();

    // 주문번호 인덱스: Order Number -> Order ID
    private final ConcurrentHashMap<String, Long> orderNumberIndex = new ConcurrentHashMap<>();

    private final static AtomicLong sequence = new AtomicLong(1);

    @Override
    public Order save(Order order) {
        if (order.getOrderId() == null) {
            Order newOrder = new Order(
                    sequence.incrementAndGet(), order.getOrderNumber(), order.getUserId(), order.getTotalAmount(),
                    order.getDiscountAmount(), order.getFinalAmount(), order.getCouponId(),
                    order.getOrderStatus(), order.getExpiresAt()
            );
            storage.put(newOrder.getOrderId(), newOrder);
            orderNumberIndex.put(order.getOrderNumber(), order.getOrderId());
            return newOrder;
        }

        storage.put(order.getOrderId(), order);
        orderNumberIndex.put(order.getOrderNumber(), order.getOrderId());
        return order;
    }

    @Override
    public Optional<Order> findById(Long orderId) {
        return Optional.ofNullable(storage.get(orderId));
    }

    @Override
    public Optional<Order> findByOrderNumber(String orderNumber) {
        Long orderId = orderNumberIndex.get(orderNumber);
        if (orderId == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(storage.get(orderId));
    }

    @Override
    public List<Order> findByUserId(Long userId) {
        return storage.values().stream()
                .filter(order -> order.getUserId().equals(userId))
                .sorted((o1, o2) -> o2.getCreatedAt().compareTo(o1.getCreatedAt())) // 최신순
                .collect(Collectors.toList());
    }

    @Override
    public List<Order> findByUserIdAndStatus(Long userId, OrderStatus status) {
        return storage.values().stream()
                .filter(order -> order.getUserId().equals(userId))
                .filter(order -> order.getOrderStatus() == status)
                .sorted((o1, o2) -> o2.getCreatedAt().compareTo(o1.getCreatedAt())) // 최신순
                .collect(Collectors.toList());
    }

    @Override
    public List<Order> findExpiredOrders(LocalDateTime currentTime) {
        return storage.values().stream()
                .filter(order -> order.getOrderStatus() == OrderStatus.PENDING)
                .filter(order -> order.getExpiresAt().isBefore(currentTime))
                .collect(Collectors.toList());
    }

    @Override
    public List<Order> findRecentOrdersByProductOption(Long productOptionId, int days) {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(days);

        return storage.values().stream()
                .filter(order -> order.getOrderStatus() == OrderStatus.PAID)
                .filter(order -> order.getCreatedAt().isAfter(cutoffDate))
                .collect(Collectors.toList());
    }
}
