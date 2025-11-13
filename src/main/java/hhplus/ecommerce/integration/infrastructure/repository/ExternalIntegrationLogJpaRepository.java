package hhplus.ecommerce.integration.infrastructure.repository;

import hhplus.ecommerce.integration.domain.model.ExternalIntegrationLog;
import hhplus.ecommerce.integration.domain.model.IntegrationType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ExternalIntegrationLogJpaRepository extends JpaRepository<ExternalIntegrationLog, Long> {

    // 주문별 연동 로그 조회 (최신순)
    @Query("SELECT eil FROM ExternalIntegrationLog eil WHERE eil.orderId = :orderId ORDER BY eil.createdAt DESC")
    List<ExternalIntegrationLog> findByOrderIdOrderByCreatedAtDesc(@Param("orderId") Long orderId);

    // 연동 타입별 로그 조회
    @Query("SELECT eil FROM ExternalIntegrationLog eil WHERE eil.integrationType = :type ORDER BY eil.createdAt DESC")
    List<ExternalIntegrationLog> findByIntegrationType(@Param("type") IntegrationType type);

    // 실패한 연동 로그 조회 (재시도 대상)
    @Query("SELECT eil FROM ExternalIntegrationLog eil WHERE eil.isSuccess = false AND eil.retryCount < :maxRetry ORDER BY eil.createdAt ASC")
    List<ExternalIntegrationLog> findFailedLogsForRetry(@Param("maxRetry") int maxRetry);

    // 주문별 + 연동 타입별 로그 조회
    @Query("SELECT eil FROM ExternalIntegrationLog eil WHERE eil.orderId = :orderId AND eil.integrationType = :type ORDER BY eil.createdAt DESC")
    List<ExternalIntegrationLog> findByOrderIdAndType(@Param("orderId") Long orderId, @Param("type") IntegrationType type);
}
