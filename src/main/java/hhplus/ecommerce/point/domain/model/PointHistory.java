package hhplus.ecommerce.point.domain.model;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "point_histories", indexes = {
    @Index(name = "idx_user_created", columnList = "user_id, created_at"),
    @Index(name = "idx_created_at", columnList = "created_at"),
    @Index(name = "idx_user_type_created", columnList = "user_id, transaction_type, created_at")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PointHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long pointHistoryId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false, length = 20)
    private TransactionType transactionType;

    @Column(name = "amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(name = "balance_after", nullable = false, precision = 15, scale = 2)
    private BigDecimal balanceAfter;

    @Column(name = "order_id")
    private Long orderId;

    @Column(name = "description", length = 200)
    private String description;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

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
    }
}
