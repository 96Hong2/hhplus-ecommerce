package hhplus.ecommerce.unitTest.order.repository;

import hhplus.ecommerce.order.domain.model.Order;
import hhplus.ecommerce.order.domain.model.OrderItem;
import hhplus.ecommerce.order.domain.model.OrderItemStatus;
import hhplus.ecommerce.order.domain.model.OrderStatus;
import hhplus.ecommerce.order.domain.model.PaymentMethod;
import hhplus.ecommerce.order.infrastructure.repository.OrderItemJpaRepository;
import hhplus.ecommerce.order.infrastructure.repository.OrderJpaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class OrderJpaRepositoryTest {

    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0");

    @DynamicPropertySource
    static void overrideProps(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
    }

    @Autowired
    private OrderJpaRepository orderJpaRepository;

    @Autowired
    private OrderItemJpaRepository orderItemJpaRepository;

    @Test
    @DisplayName("JPA: 주문 저장, 주문번호 조회, 사용자별/상태별 조회")
    void basicOrderQueries() {
        String orderNumber1 = Order.generateOrderNumber();
        String orderNumber2 = Order.generateOrderNumber();

        Order o1 = Order.create(orderNumber1, 1L, BigDecimal.valueOf(10000), BigDecimal.ZERO, null);
        Order o2 = Order.create(orderNumber2, 1L, BigDecimal.valueOf(20000), BigDecimal.ZERO, null);

        Order saved1 = orderJpaRepository.save(o1);
        Order saved2 = orderJpaRepository.save(o2);

        Optional<Order> byOrderNumber = orderJpaRepository.findByOrderNumber(orderNumber1);
        assertThat(byOrderNumber).isPresent();

        List<Order> byUser = orderJpaRepository.findByUserIdOrderByCreatedAtDesc(1L);
        assertThat(byUser).hasSize(2);

        List<Order> byUserAndStatus = orderJpaRepository.findByUserIdAndStatus(1L, OrderStatus.PENDING);
        assertThat(byUserAndStatus).hasSize(2);

        // 특정 상품 옵션의 최근 주문 조회를 위한 OrderItem 저장
        // findRecentOrdersByProductOption은 PAID 상태의 주문만 조회하므로 상태 변경 필요
        saved1 = saved1.payWithMethod(PaymentMethod.CREDIT);
        orderJpaRepository.save(saved1);

        OrderItem item = OrderItem.create(saved1.getOrderId(), 10L, 100L, "상품", "옵션", BigDecimal.valueOf(5000), 2);
        OrderItem savedItem = orderItemJpaRepository.save(item);
        assertThat(savedItem.getItemStatus()).isEqualTo(OrderItemStatus.PREPARING);

        List<Order> recent = orderJpaRepository.findRecentOrdersByProductOption(100L, LocalDateTime.now().minusDays(1));
        assertThat(recent).extracting(Order::getOrderId).contains(saved1.getOrderId());
    }
}

