package hhplus.ecommerce.user.domain.model;

import hhplus.ecommerce.common.domain.constants.BusinessConstants;
import hhplus.ecommerce.common.domain.exception.UserException;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
public class User {
    private final Long userId;
    private String username;
    private BigDecimal pointBalance;
    private final UserRole role;
    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public User(Long userId, String username, BigDecimal pointBalance, UserRole role) {
        this.userId = userId;
        this.username = username;
        this.pointBalance = pointBalance;
        this.role = role;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 새로운 유저를 생성한다.
     * @param username 사용자명 (unique)
     * @param role 역할 (CUSTOMER 또는 ADMIN)
     * @return 생성된 유저 (userId는 null, Repository에서 할당)
     */
    public static User create(String username, UserRole role) {

        if (username == null || username.isBlank()) {
            throw UserException.creationFailed("사용자명은 필수입니다.");
        }
        if (role == null) {
            throw UserException.creationFailed("역할은 필수입니다.");
        }

        return new User(null, username, BigDecimal.ZERO, role);
    }

    /**
     * 포인트를 사용한다.
     * @param amount 사용할 포인트
     * @throws hhplus.ecommerce.common.domain.exception.UserException 잔액 부족 또는 유효하지 않은 금액
     */
    public void usePoint(BigDecimal amount) {
        validateAmount(amount);

        if (this.pointBalance.compareTo(amount) < 0) {
            throw UserException.creationFailed("포인트 잔액이 부족합니다.");
        }

        this.pointBalance = this.pointBalance.subtract(amount);
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 포인트를 충전한다.
     * @param amount 충전할 포인트 (최소 1,000원)
     * @throws hhplus.ecommerce.common.domain.exception.UserException 유효하지 않은 금액
     */
    public void chargePoint(BigDecimal amount) {
        validateAmount(amount);

        // 비즈니스 규칙 검증 - 최소 충전 금액
        if (amount.compareTo(BusinessConstants.MIN_CHARGE_AMOUNT) < 0) {
            throw UserException.creationFailed(
                    String.format("최소 충전 금액은 %s 원입니다.", BusinessConstants.MIN_CHARGE_AMOUNT)
            );
        }

        this.pointBalance = this.pointBalance.add(amount);
        this.updatedAt = LocalDateTime.now();
    }

    private void validateAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw UserException.creationFailed("금액은 0보다 커야 합니다.");
        }
    }
}
