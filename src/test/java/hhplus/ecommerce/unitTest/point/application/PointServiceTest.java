package hhplus.ecommerce.unitTest.point.application;

import hhplus.ecommerce.common.domain.constants.BusinessConstants;
import hhplus.ecommerce.common.domain.exception.PointException;
import hhplus.ecommerce.point.application.service.PointService;
import hhplus.ecommerce.point.domain.model.PointHistory;
import hhplus.ecommerce.point.domain.model.TransactionType;
import hhplus.ecommerce.point.domain.repository.PointHistoryRepository;
import hhplus.ecommerce.user.domain.model.User;
import hhplus.ecommerce.user.domain.model.UserRole;
import hhplus.ecommerce.user.domain.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PointServiceTest {

    @Mock
    private PointHistoryRepository pointHistoryRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private PointService pointService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User(1L, "testuser",
                BigDecimal.valueOf(10000), UserRole.CUSTOMER);
    }

    @Test
    @DisplayName("포인트를 정상적으로 충전할 수 있다")
    void chargePoint() {
        // given
        BigDecimal chargeAmount = BigDecimal.valueOf(5000);
        BigDecimal expectedBalance = testUser.getPointBalance().add(chargeAmount);

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(pointHistoryRepository.save(any(PointHistory.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // when
        PointHistory result = pointService.chargePoint(1L, chargeAmount, "포인트 충전");

        // then
        assertThat(result).isNotNull();
        assertThat(result.getAmount()).isEqualByComparingTo(chargeAmount);
        verify(userRepository, times(1)).findById(1L);
        verify(userRepository, times(1)).save(any(User.class));
        verify(pointHistoryRepository, times(1)).save(any(PointHistory.class));
    }

    @Test
    @DisplayName("최소 충전금액 미만으로 충전 시도 시 예외가 발생한다")
    void chargePointBelowMinimum() {
        // given
        BigDecimal lowAmount = BusinessConstants.MIN_CHARGE_AMOUNT.subtract(BigDecimal.ONE);

        // when & then
        assertThatThrownBy(() -> pointService.chargePoint(1L, lowAmount, "포인트 충전"))
                .isInstanceOf(PointException.class);

        verify(userRepository, never()).findById(any());
    }

    @Test
    @DisplayName("존재하지 않는 사용자의 포인트 충전 시 예외가 발생한다")
    void chargePointUserNotFound() {
        // given
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> pointService.chargePoint(999L, BigDecimal.valueOf(5000), "충전"))
                .isInstanceOf(PointException.class);

        verify(userRepository, times(1)).findById(999L);
        verify(pointHistoryRepository, never()).save(any());
    }

    @Test
    @DisplayName("포인트를 정상적으로 사용할 수 있다")
    void usePoint() {
        // given
        BigDecimal useAmount = BigDecimal.valueOf(3000);

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(pointHistoryRepository.save(any(PointHistory.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // when
        PointHistory result = pointService.usePoint(1L, useAmount, 1L, "주문 결제");

        // then
        assertThat(result).isNotNull();
        assertThat(result.getAmount()).isEqualByComparingTo(useAmount);
        assertThat(result.getOrderId()).isEqualTo(1L);
        verify(userRepository, times(1)).findById(1L);
        verify(userRepository, times(1)).save(any(User.class));
        verify(pointHistoryRepository, times(1)).save(any(PointHistory.class));
    }

    @Test
    @DisplayName("잔액보다 많은 포인트 사용 시도 시 예외가 발생한다")
    void usePointInsufficientBalance() {
        // given
        BigDecimal excessiveAmount = testUser.getPointBalance().add(BigDecimal.valueOf(1000));

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        // when & then
        assertThatThrownBy(() -> pointService.usePoint(1L, excessiveAmount, 1L, "주문 결제"))
                .isInstanceOf(PointException.class);

        verify(userRepository, times(1)).findById(1L);
        verify(pointHistoryRepository, never()).save(any());
    }

    @Test
    @DisplayName("0원 이하의 포인트 사용 시도 시 예외가 발생한다")
    void usePointZeroOrNegative() {
        // when & then
        assertThatThrownBy(() -> pointService.usePoint(1L, BigDecimal.ZERO, 1L, "결제"))
                .isInstanceOf(PointException.class);

        assertThatThrownBy(() -> pointService.usePoint(1L, BigDecimal.valueOf(-1000), 1L, "결제"))
                .isInstanceOf(PointException.class);

        verify(userRepository, never()).findById(any());
    }

    @Test
    @DisplayName("포인트 히스토리를 조회할 수 있다")
    void getPointHistory() {
        // given
        List<PointHistory> expectedHistory = List.of(
                new PointHistory(1L, 1L, BigDecimal.valueOf(5000), BigDecimal.valueOf(15000), "충전"),
                new PointHistory(2L, 1L, BigDecimal.valueOf(3000), BigDecimal.valueOf(12000), 1L, "사용")
        );

        when(pointHistoryRepository.findByUserId(1L)).thenReturn(expectedHistory);

        // when
        List<PointHistory> result = pointService.getPointHistory(1L, null);

        // then
        assertThat(result).hasSize(2);
        verify(pointHistoryRepository, times(1)).findByUserId(1L);
    }

    @Test
    @DisplayName("포인트 히스토리를 거래 유형별로 조회할 수 있다")
    void getPointHistoryByType() {
        // given
        List<PointHistory> chargeHistory = List.of(
                new PointHistory(1L, 1L, BigDecimal.valueOf(5000), BigDecimal.valueOf(15000), "충전")
        );

        when(pointHistoryRepository.findByUserIdAndTransactionType(1L, TransactionType.CHARGE))
                .thenReturn(chargeHistory);

        // when
        List<PointHistory> result = pointService.getPointHistory(1L, TransactionType.CHARGE);

        // then
        assertThat(result).hasSize(1);
        verify(pointHistoryRepository, times(1))
                .findByUserIdAndTransactionType(1L, TransactionType.CHARGE);
    }
}
