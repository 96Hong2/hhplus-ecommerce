package hhplus.ecommerce.unitTest.order.repository;

import hhplus.ecommerce.order.domain.model.Order;
import hhplus.ecommerce.order.domain.model.OrderStatus;
import hhplus.ecommerce.order.infrastructure.repository.InMemoryOrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class OrderRepositoryTest {

    private InMemoryOrderRepository orderRepository;

    @BeforeEach
    void setUp() {
        orderRepository = new InMemoryOrderRepository();
    }

    @Test
    @DisplayName("주문을 저장할 수 있다")
    void saveOrder() {
        Order order = Order.create(
                "ORD-001",
                1L,
                BigDecimal.valueOf(50000),
                BigDecimal.valueOf(5000),
                null
        );

        Order savedOrder = orderRepository.save(order);

        assertThat(savedOrder).isNotNull();
        assertThat(savedOrder.getOrderId()).isNotNull();
        assertThat(savedOrder.getOrderNumber()).isEqualTo("ORD-001");
    }

    @Test
    @DisplayName("ID로 주문을 조회할 수 있다")
    void findById() {
        Order order = Order.create(
                "ORD-001",
                1L,
                BigDecimal.valueOf(50000),
                BigDecimal.valueOf(5000),
                null
        );
        Order savedOrder = orderRepository.save(order);

        Optional<Order> foundOrder = orderRepository.findById(savedOrder.getOrderId());

        assertThat(foundOrder).isPresent();
        assertThat(foundOrder.get().getOrderNumber()).isEqualTo("ORD-001");
    }

    @Test
    @DisplayName("사용자 ID로 주문 목록을 조회할 수 있다")
    void findAllByUserId() {
        orderRepository.save(Order.create("ORD-001", 1L, BigDecimal.valueOf(10000), BigDecimal.ZERO, null));
        orderRepository.save(Order.create("ORD-002", 1L, BigDecimal.valueOf(20000), BigDecimal.ZERO, null));
        orderRepository.save(Order.create("ORD-003", 2L, BigDecimal.valueOf(30000), BigDecimal.ZERO, null));

        // 메서드명 변경 findAllByUserId → findByUserId
        List<Order> orders = orderRepository.findByUserId(1L);

        assertThat(orders).hasSize(2);
    }

    @Test
    @DisplayName("주문 상태로 주문 목록을 조회할 수 있다")
    void findAllByUserIdAndOrderStatus() {
        Order order1 = Order.create("ORD-001", 1L, BigDecimal.valueOf(10000), BigDecimal.ZERO, null);
        Order order2 = Order.create("ORD-002", 1L, BigDecimal.valueOf(20000), BigDecimal.ZERO, null);

        order2 = order2.pay();

        orderRepository.save(order1);
        orderRepository.save(order2);

        // 메서드명 변경 findAllByUserIdAndOrderStatus → findByUserIdAndStatus
        List<Order> pendingOrders = orderRepository.findByUserIdAndStatus(1L, OrderStatus.PENDING);
        List<Order> paidOrders = orderRepository.findByUserIdAndStatus(1L, OrderStatus.PAID);

        assertThat(pendingOrders).hasSize(1);
        assertThat(paidOrders).hasSize(1);
    }

    @Test
    @DisplayName("주문 번호로 주문을 조회할 수 있다")
    void findByOrderNumber() {
        Order order = Order.create(
                "ORD-001",
                1L,
                BigDecimal.valueOf(50000),
                BigDecimal.ZERO,
                null
        );
        orderRepository.save(order);

        Optional<Order> foundOrder = orderRepository.findByOrderNumber("ORD-001");

        assertThat(foundOrder).isPresent();
        assertThat(foundOrder.get().getUserId()).isEqualTo(1L);
    }
}
