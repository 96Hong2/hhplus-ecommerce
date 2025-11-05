package hhplus.ecommerce.unitTest.domain.model.user;

import hhplus.ecommerce.user.domain.model.User;
import hhplus.ecommerce.user.domain.model.UserRole;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class UserTest {

    @Test
    @DisplayName("포인트 사용 시 잔액이 정상적으로 차감된다.")
    void userUsePoint() {
        // given : 충분한 포인트를 가진 유저
        User user = new User(1L, "test", BigDecimal.valueOf(1000), UserRole.CUSTOMER);

        // when : 포인트 사용
        user.usePoint(BigDecimal.valueOf(100));

        // then : 사용한만큼 잔액 차감
        assertThat(user.getPointBalance()).isEqualByComparingTo(BigDecimal.valueOf(900));
    }

    @Test
    @DisplayName("포인트 사용 시 잔액이 부족하면 예외가 발생한다.")
    void userInsufficientPoint() {
        // given
        User user = new User(1L, "test", BigDecimal.valueOf(1000), UserRole.CUSTOMER);

        // when & then
        assertThatThrownBy(() -> user.usePoint(BigDecimal.valueOf(10000)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("포인트 잔액이 부족합니다.");
    }

    @Test
    @DisplayName("포인트를 충전하면 잔액이 정상적으로 증가한다.")
    void userChargePoint() {
        // given
        User user = new User(1L, "test", BigDecimal.valueOf(1000), UserRole.CUSTOMER);

        // when
        BigDecimal chargeAmount = BigDecimal.valueOf(100);
        user.chargePoint(chargeAmount);

        // then
        assertThat(user.getPointBalance()).isEqualByComparingTo(BigDecimal.valueOf(1100));
    }

    @Test
    @DisplayName("포인트 충전 금액이 1000원 미만이면 예외가 발생한다.")
    void userChargeLessPoint() {
        // given
        User user = new User(1L, "test", BigDecimal.valueOf(1000), UserRole.CUSTOMER);

        // when & then
        assertThatThrownBy(() -> user.chargePoint(BigDecimal.valueOf(999)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("최소 충전 금액은 1,000원입니다.");
    }

    @ParameterizedTest
    @DisplayName("포인트 사용 금액이 0원 이하이면 예외가 발생한다.")
    @ValueSource(strings = {"0", "-1000", "-999999"})
    void userUsePointLessThenZero(String amount) {
        // given
        User user = new User(1L, "test", BigDecimal.valueOf(1000), UserRole.CUSTOMER);

        // when & then
        assertThatThrownBy(() -> user.usePoint(new BigDecimal(amount)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("금액은 0보다 커야 합니다.");
    }
}
