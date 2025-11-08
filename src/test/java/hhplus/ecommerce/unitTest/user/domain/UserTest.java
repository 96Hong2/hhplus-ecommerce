package hhplus.ecommerce.unitTest.user.domain;

import hhplus.ecommerce.common.domain.constants.BusinessConstants;
import hhplus.ecommerce.common.domain.exception.UserException;
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
    @DisplayName("User.create()로 신규 유저를 생성하면 pointBalance가 0으로 초기화된다.")
    void createUser() {
        // when
        User user = User.create("testUser", UserRole.CUSTOMER);

        // then
        assertThat(user.getUserId()).isNull(); // userId는 Repository에서 할당
        assertThat(user.getUsername()).isEqualTo("testUser");
        assertThat(user.getRole()).isEqualTo(UserRole.CUSTOMER);
        assertThat(user.getPointBalance()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("User.create() 시 username이 null이면 예외가 발생한다.")
    void createUserWithNullUsername() {
        // when & then
        assertThatThrownBy(() -> User.create(null, UserRole.CUSTOMER))
                .isInstanceOf(UserException.class)
                .hasMessageContaining("사용자명은 필수입니다.");
    }

    @Test
    @DisplayName("User.create() 시 username이 빈 문자열이면 예외가 발생한다.")
    void createUserWithBlankUsername() {
        // when & then
        assertThatThrownBy(() -> User.create("   ", UserRole.CUSTOMER))
                .isInstanceOf(UserException.class)
                .hasMessageContaining("사용자명은 필수입니다.");
    }

    @Test
    @DisplayName("User.create() 시 role이 null이면 예외가 발생한다.")
    void createUserWithNullRole() {
        // when & then
        assertThatThrownBy(() -> User.create("testUser", null))
                .isInstanceOf(UserException.class)
                .hasMessageContaining("역할은 필수입니다.");
    }

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
                .isInstanceOf(UserException.class)
                .hasMessageContaining("포인트 잔액이 부족합니다.");
    }

    @Test
    @DisplayName("포인트를 충전하면 잔액이 정상적으로 증가한다.")
    void userChargePoint() {
        // given
        User user = new User(1L, "test", BigDecimal.valueOf(1000), UserRole.CUSTOMER);

        // when
        BigDecimal chargeAmount = BigDecimal.valueOf(1000);
        user.chargePoint(chargeAmount);

        // then
        assertThat(user.getPointBalance()).isEqualByComparingTo(BigDecimal.valueOf(2000));
    }

    @Test
    @DisplayName("포인트 충전 금액이 최소 충전 금액 미만이면 예외가 발생한다.")
    void userChargeLessPoint() {
        // given
        User user = new User(1L, "test", BigDecimal.valueOf(1000), UserRole.CUSTOMER);

        // when & then
        assertThatThrownBy(() -> user.chargePoint(BigDecimal.valueOf(999)))
                .isInstanceOf(UserException.class)
                .hasMessageContaining(String.format("최소 충전 금액은 %s 원입니다.", BusinessConstants.MIN_CHARGE_AMOUNT));
    }

    @ParameterizedTest
    @DisplayName("포인트 사용 금액이 0원 이하이면 예외가 발생한다.")
    @ValueSource(strings = {"0", "-1000", "-999999"})
    void userUsePointLessThenZero(String amount) {
        // given
        User user = new User(1L, "test", BigDecimal.valueOf(1000), UserRole.CUSTOMER);

        // when & then
        assertThatThrownBy(() -> user.usePoint(new BigDecimal(amount)))
                .isInstanceOf(UserException.class)
                .hasMessageContaining("금액은 0보다 커야 합니다.");
    }

    @ParameterizedTest
    @DisplayName("포인트 충전 금액이 0원 이하이면 예외가 발생한다.")
    @ValueSource(strings = {"0", "-1000", "-999999"})
    void userChargePointLessThenZero(String amount) {
        // given
        User user = new User(1L, "test", BigDecimal.valueOf(1000), UserRole.CUSTOMER);

        // when & then
        assertThatThrownBy(() -> user.chargePoint(new BigDecimal(amount)))
                .isInstanceOf(UserException.class)
                .hasMessageContaining("금액은 0보다 커야 합니다.");
    }
}
