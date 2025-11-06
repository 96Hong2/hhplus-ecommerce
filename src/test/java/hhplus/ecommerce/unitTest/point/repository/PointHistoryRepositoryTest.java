package hhplus.ecommerce.unitTest.point.repository;

import hhplus.ecommerce.point.domain.model.PointHistory;
import hhplus.ecommerce.point.domain.model.TransactionType;
import hhplus.ecommerce.point.infrastructure.repository.InMemoryPointHistoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class PointHistoryRepositoryTest {

    private InMemoryPointHistoryRepository pointHistoryRepository;

    @BeforeEach
    void setUp() {
        pointHistoryRepository = new InMemoryPointHistoryRepository();
    }

    @Test
    @DisplayName("포인트 거래 내역을 저장할 수 있다")
    void savePointHistory() {
        PointHistory history = new PointHistory(
                null,
                1L,
                BigDecimal.valueOf(10000),
                BigDecimal.valueOf(10000),
                "포인트 충전"
        );

        PointHistory savedHistory = pointHistoryRepository.save(history);

        assertThat(savedHistory).isNotNull();
        assertThat(savedHistory.getPointHistoryId()).isNotNull();
        assertThat(savedHistory.getAmount()).isEqualTo(BigDecimal.valueOf(10000));
    }

    @Test
    @DisplayName("ID로 포인트 거래 내역을 조회할 수 있다")
    void findById() {
        PointHistory history = new PointHistory(
                null,
                1L,
                BigDecimal.valueOf(10000),
                BigDecimal.valueOf(10000),
                "포인트 충전"
        );
        PointHistory savedHistory = pointHistoryRepository.save(history);

        Optional<PointHistory> foundHistory = pointHistoryRepository.findById(savedHistory.getPointHistoryId());

        assertThat(foundHistory).isPresent();
        assertThat(foundHistory.get().getDescription()).isEqualTo("포인트 충전");
    }

    @Test
    @DisplayName("사용자 ID로 포인트 거래 내역을 조회할 수 있다")
    void findByUserId() {
        pointHistoryRepository.save(new PointHistory(null, 1L, BigDecimal.valueOf(10000), BigDecimal.valueOf(10000), "충전1"));
        pointHistoryRepository.save(new PointHistory(null, 1L, BigDecimal.valueOf(5000), BigDecimal.valueOf(15000), "충전2"));
        pointHistoryRepository.save(new PointHistory(null, 2L, BigDecimal.valueOf(20000), BigDecimal.valueOf(20000), "충전3"));

        List<PointHistory> histories = pointHistoryRepository.findByUserId(1L);

        assertThat(histories).hasSize(2);
    }

    @Test
    @DisplayName("사용자 ID와 거래 유형으로 포인트 거래 내역을 조회할 수 있다")
    void findByUserIdAndTransactionType() {
        pointHistoryRepository.save(new PointHistory(null, 1L, BigDecimal.valueOf(10000), BigDecimal.valueOf(10000), "충전"));
        pointHistoryRepository.save(new PointHistory(null, 1L, BigDecimal.valueOf(5000), BigDecimal.valueOf(5000), 1L, "사용"));

        List<PointHistory> chargeHistories = pointHistoryRepository.findByUserIdAndTransactionType(1L, TransactionType.CHARGE);
        List<PointHistory> useHistories = pointHistoryRepository.findByUserIdAndTransactionType(1L, TransactionType.USE);

        assertThat(chargeHistories).hasSize(1);
        assertThat(useHistories).hasSize(1);
    }

    @Test
    @DisplayName("주문 ID로 포인트 거래 내역을 조회할 수 있다")
    void findByOrderId() {
        pointHistoryRepository.save(new PointHistory(null, 1L, BigDecimal.valueOf(5000), BigDecimal.valueOf(5000), 100L, "주문 결제"));
        pointHistoryRepository.save(new PointHistory(null, 1L, BigDecimal.valueOf(3000), BigDecimal.valueOf(2000), 101L, "주문 결제"));

        Optional<PointHistory> history = pointHistoryRepository.findByOrderId(100L);

        assertThat(history).isPresent();
        assertThat(history.get().getAmount()).isEqualTo(BigDecimal.valueOf(5000));
    }
}
