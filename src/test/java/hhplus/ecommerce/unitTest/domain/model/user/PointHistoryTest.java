package hhplus.ecommerce.unitTest.domain.model.user;

import hhplus.ecommerce.point.domain.model.PointHistory;
import hhplus.ecommerce.point.domain.model.TransactionType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class PointHistoryTest {

    @Test
    @DisplayName("포인트 사용 시 히스토리를 생성하면 모든 필드가 올바르게 초기화된다")
    void createPointHistoryForUse() {
        // given
        Long pointHistoryId = 1L;
        Long userId = 100L;
        TransactionType transactionType = TransactionType.USE;
        BigDecimal amount = BigDecimal.valueOf(1000);
        BigDecimal balanceAfter = BigDecimal.valueOf(9000);
        Long orderId = 200L;
        String description = "상품 구매";

        // when
        PointHistory pointHistory = new PointHistory(
                pointHistoryId, userId, transactionType, amount, balanceAfter, orderId, description
        );

        // then
        assertThat(pointHistory.getPointHistoryId()).isEqualTo(pointHistoryId);
        assertThat(pointHistory.getUserId()).isEqualTo(userId);
        assertThat(pointHistory.getTransactionType()).isEqualTo(transactionType);
        assertThat(pointHistory.getAmount()).isEqualTo(amount);
        assertThat(pointHistory.getBalanceAfter()).isEqualTo(balanceAfter);
        assertThat(pointHistory.getOrderId()).isEqualTo(orderId);
        assertThat(pointHistory.getDescription()).isEqualTo(description);
        assertThat(pointHistory.getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("포인트 충전 시 히스토리를 생성하면 orderId가 null이고 나머지 필드는 올바르게 초기화된다")
    void createPointHistoryForCharge() {
        // given
        Long pointHistoryId = 2L;
        Long userId = 100L;
        TransactionType transactionType = TransactionType.CHARGE;
        BigDecimal amount = BigDecimal.valueOf(5000);
        BigDecimal balanceAfter = BigDecimal.valueOf(15000);
        String description = "포인트 충전";

        // when
        PointHistory pointHistory = new PointHistory(pointHistoryId, userId, amount, balanceAfter, description);

        // then
        assertThat(pointHistory.getPointHistoryId()).isEqualTo(pointHistoryId);
        assertThat(pointHistory.getUserId()).isEqualTo(userId);
        assertThat(pointHistory.getTransactionType()).isEqualTo(transactionType);
        assertThat(pointHistory.getAmount()).isEqualTo(amount);
        assertThat(pointHistory.getBalanceAfter()).isEqualTo(balanceAfter);
        assertThat(pointHistory.getOrderId()).isNull();
        assertThat(pointHistory.getDescription()).isEqualTo(description);
        assertThat(pointHistory.getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("TransactionType.USE로 히스토리를 생성하면 거래 타입이 USE이다")
    void createPointHistoryWithUseTransactionType() {
        // given
        TransactionType transactionType = TransactionType.USE;

        // when
        PointHistory pointHistory = new PointHistory(
                1L, 100L, transactionType, BigDecimal.valueOf(1000),
                BigDecimal.valueOf(9000), 200L, "상품 구매"
        );

        // then
        assertThat(pointHistory.getTransactionType()).isEqualTo(TransactionType.USE);
        assertThat(pointHistory.getTransactionType().getDescription()).isEqualTo("사용");
    }

    @Test
    @DisplayName("TransactionType.CHARGE로 히스토리를 생성하면 거래 타입이 CHARGE이다")
    void createPointHistoryWithChargeTransactionType() {
        // given
        TransactionType transactionType = TransactionType.CHARGE;

        // when
        PointHistory pointHistory = new PointHistory(
                1L, 100L, BigDecimal.valueOf(5000), BigDecimal.valueOf(15000), "포인트 충전"
        );

        // then
        assertThat(pointHistory.getTransactionType()).isEqualTo(TransactionType.CHARGE);
        assertThat(pointHistory.getTransactionType().getDescription()).isEqualTo("충전");
    }

    @Test
    @DisplayName("거래 금액이 음수일 때도 히스토리가 생성된다 (검증은 상위 레이어 책임)")
    void createPointHistoryWithNegativeAmount() {
        // given
        BigDecimal negativeAmount = BigDecimal.valueOf(-1000);

        // when
        PointHistory pointHistory = new PointHistory(
                1L, 100L, TransactionType.USE, negativeAmount,
                BigDecimal.valueOf(9000), 200L, "테스트"
        );

        // then
        assertThat(pointHistory.getAmount()).isEqualTo(negativeAmount);
    }

    @Test
    @DisplayName("balanceAfter가 음수일 때도 히스토리가 생성된다 (검증은 상위 레이어 책임)")
    void createPointHistoryWithNegativeBalance() {
        // given
        BigDecimal negativeBalance = BigDecimal.valueOf(-1000);

        // when
        PointHistory pointHistory = new PointHistory(
                1L, 100L, TransactionType.USE, BigDecimal.valueOf(2000),
                negativeBalance, 200L, "테스트"
        );

        // then
        assertThat(pointHistory.getBalanceAfter()).isEqualTo(negativeBalance);
    }
}
