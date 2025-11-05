package hhplus.ecommerce.user.domain.model;

import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
public class User {
    // Getter
    private final Long userId;
    private String username;
    private BigDecimal pointBalance;
    private final UserRole role;
    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // 생성자
    public User(Long userId, String username, BigDecimal pointBalance, UserRole role) {
        this.userId = userId;
        this.username = username;
        this.pointBalance = pointBalance;
        this.role = role;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    // 비즈니스 규칙: 포인트 사용
    public void usePoint(BigDecimal amount) {
        validateAmount(amount);

        if (this.pointBalance.compareTo(amount) < 0) {
            throw new IllegalArgumentException("포인트 잔액이 부족합니다.");
        }

        this.pointBalance = this.pointBalance.subtract(amount);
        this.updatedAt = LocalDateTime.now();
    }

    // 비즈니스 규칙: 포인트 충전
    public void chargePoint(BigDecimal amount) {
        validateAmount(amount);

        if (amount.compareTo(new BigDecimal("1000")) < 0) {
            throw new IllegalArgumentException("최소 충전 금액은 1,000원입니다.");
        }

        this.pointBalance = this.pointBalance.add(amount);
        this.updatedAt = LocalDateTime.now();
    }

    private void validateAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("금액은 0보다 커야 합니다.");
        }
    }

}
