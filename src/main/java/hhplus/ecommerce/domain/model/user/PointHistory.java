package hhplus.ecommerce.domain.model.user;

import hhplus.ecommerce.domain.model.point.TransactionType;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class PointHistory {
    private Long pointHistoryId;
    private Long userId;
    private TransactionType transactionType;
    private BigDecimal amount;
    private BigDecimal balanceAfter;
    private Long orderId;
    private String description;
    private LocalDateTime createdAt;

    // 생성자 (포인트 사용 시)
    public PointHistory(Long pointHistoryId, Long userId, TransactionType transactionType,
                        BigDecimal amount, BigDecimal balanceAfter, Long orderId, String description) {
        this.pointHistoryId = pointHistoryId;
        this.userId = userId;
        this.transactionType = transactionType;
        this.amount = amount;
        this.balanceAfter = balanceAfter;
        this.orderId = orderId;
        this.description = description;
        this.createdAt = LocalDateTime.now();
    }

    // 생성자 (포인트 충전 시)
    public PointHistory(Long pointHistoryId, Long userId, TransactionType transactionType,
                        BigDecimal amount, BigDecimal balanceAfter, String description) {
        this(pointHistoryId, userId, transactionType, amount, balanceAfter, null, description);
    }

    // Getter
    public Long getPointHistoryId() {
        return pointHistoryId;
    }

    public Long getUserId() {
        return userId;
    }

    public TransactionType getTransactionType() {
        return transactionType;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public BigDecimal getBalanceAfter() {
        return balanceAfter;
    }

    public Long getOrderId() {
        return orderId;
    }

    public String getDescription() {
        return description;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
