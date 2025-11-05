package hhplus.ecommerce.point.domain.model;

import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
public class PointHistory {

    private final Long pointHistoryId;
    private final Long userId;
    private final TransactionType transactionType;
    private final BigDecimal amount;
    private final BigDecimal balanceAfter; // 거래 후 잔액 스냅샷
    private final Long orderId; // 사용 시에만 존재
    private final String description;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    // 충전 생성자
    public PointHistory(Long pointHistoryId, Long userId,
                        BigDecimal amount, BigDecimal balanceAfter, String description) {
        this(pointHistoryId, userId, TransactionType.CHARGE, amount, balanceAfter, null, description);
    }

    // 사용 생성자
    public PointHistory(Long pointHistoryId, Long userId,
                        BigDecimal amount, BigDecimal balanceAfter, Long orderId, String description) {
        this(pointHistoryId, userId, TransactionType.USE, amount, balanceAfter, orderId, description);
    }

    // 전체 생성자
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
        this.updatedAt = LocalDateTime.now();
    }
}
