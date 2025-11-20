package hhplus.ecommerce.unitTest.point.repository;

import hhplus.ecommerce.point.domain.model.PointHistory;
import hhplus.ecommerce.point.domain.model.TransactionType;
import hhplus.ecommerce.point.domain.repository.PointHistoryRepository;
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
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class PointHistoryJpaRepositoryTest {

    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0");

    @DynamicPropertySource
    static void overrideProps(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
    }

    @Autowired
    private PointHistoryRepository pointHistoryJpaRepository;

    @Test
    @DisplayName("JPA: 포인트 이력 저장 및 사용자/타입/주문별 조회")
    void saveAndQueries() {
        // 사용자 1: 충전 2건, 사용 1건(주문 연계)
        pointHistoryJpaRepository.save(new PointHistory(null, 1L, TransactionType.CHARGE, BigDecimal.valueOf(10000), BigDecimal.valueOf(10000), null, "charge1"));
        pointHistoryJpaRepository.save(new PointHistory(null, 1L, TransactionType.CHARGE, BigDecimal.valueOf(5000), BigDecimal.valueOf(15000), null, "charge2"));
        pointHistoryJpaRepository.save(new PointHistory(null, 1L, TransactionType.USE, BigDecimal.valueOf(3000), BigDecimal.valueOf(12000), 99L, "use1"));

        List<PointHistory> byUser = pointHistoryJpaRepository.findByUserIdOrderByCreatedAtDesc(1L);
        assertThat(byUser).hasSize(3);

        List<PointHistory> charges = pointHistoryJpaRepository.findByUserIdAndType(1L, TransactionType.CHARGE);
        assertThat(charges).hasSize(2);

        List<PointHistory> byOrder = pointHistoryJpaRepository.findByOrderId(99L);
        assertThat(byOrder).hasSize(1);
        assertThat(byOrder.get(0).getTransactionType()).isEqualTo(TransactionType.USE);
    }
}

