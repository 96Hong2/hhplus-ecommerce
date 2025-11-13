package hhplus.ecommerce.integration.domain.model;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "external_integration_logs", indexes = {
    @Index(name = "idx_order_created", columnList = "order_id, created_at"),
    @Index(name = "idx_type_success_retry", columnList = "integration_type, is_success, retry_count"),
    @Index(name = "idx_created_at", columnList = "created_at")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ExternalIntegrationLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long logId;

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @Enumerated(EnumType.STRING)
    @Column(name = "integration_type", nullable = false, length = 50)
    private IntegrationType integrationType;

    @Column(name = "is_success", nullable = false, columnDefinition = "TINYINT(1)")
    private boolean isSuccess;

    @Column(name = "response_message", columnDefinition = "TEXT")
    private String responseMessage;

    @Column(name = "retry_count", nullable = false)
    private int retryCount;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    private ExternalIntegrationLog(Long logId, Long orderId, IntegrationType integrationType,
                                   boolean isSuccess, String responseMessage, int retryCount) {
        this.logId = logId;
        this.orderId = orderId;
        this.integrationType = integrationType;
        this.isSuccess = isSuccess;
        this.responseMessage = responseMessage;
        this.retryCount = retryCount;
    }

    public static ExternalIntegrationLog create(Long orderId, IntegrationType integrationType, String responseMessage) {
        return new ExternalIntegrationLog(null, orderId, integrationType,
                false, responseMessage, 0);
    }

    public void markSuccess(String message) {
        this.isSuccess = true;
        this.responseMessage = message;
    }

    public void markFailure(String message) {
        this.isSuccess = false;
        this.responseMessage = message;
    }

    public void incrementRetry() {
        this.retryCount += 1;
    }
}

