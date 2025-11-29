package hhplus.ecommerce.integration.application.service;

import hhplus.ecommerce.common.domain.exception.IntegrationException;
import hhplus.ecommerce.integration.domain.model.ExternalIntegrationLog;
import hhplus.ecommerce.integration.domain.model.IntegrationType;
import hhplus.ecommerce.integration.infrastructure.repository.ExternalIntegrationLogJpaRepository;
import hhplus.ecommerce.order.domain.model.Order;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 외부 시스템 연동 서비스
 * - ERP, 물류, 판매관리 시스템과의 통신을 담당
 * - 연동 로그 저장 및 재시도 관리
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExternalIntegrationService {

    private final ExternalIntegrationLogJpaRepository integrationLogRepository;
    private boolean isSimulation = false;

    /**
     * ERP 시스템으로 주문 정보 전송
     * @param order 주문 정보
     * @return 저장된 연동 로그
     *
     * @Transactional(propagation = REQUIRES_NEW): 별도의 독립적인 트랜잭션으로 실행
     * 주문 생성 트랜잭션과 분리하여 외부 시스템 연동 실패가 주문 생성에 영향을 주지 않도록 함
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ExternalIntegrationLog sendOrderToERP(Order order) {
        log.info("ERP 시스템으로 주문 전송 시작 - OrderId: {}, OrderNumber: {}",
                order.getOrderId(), order.getOrderNumber());

        ExternalIntegrationLog log = ExternalIntegrationLog.create(
                order.getOrderId(),
                IntegrationType.ERP,
                "ERP 주문 정보 전송 시도"
        );

        try {
            // 외부 시스템 API 호출 (시뮬레이션)
            sendToExternalSystem(order);

            log.markSuccess("ERP 시스템 전송 성공");
            ExternalIntegrationLog savedLog = integrationLogRepository.save(log);

            this.log.info("ERP 시스템 전송 성공 - OrderId: {}", order.getOrderId());
            return savedLog;

        } catch (Exception e) {
            log.incrementRetry();
            log.markFailure("ERP 시스템 전송 실패: " + e.getMessage());
            ExternalIntegrationLog savedLog = integrationLogRepository.save(log);

            this.log.error("ERP 시스템 전송 실패 - OrderId: {}, Error: {}",
                    order.getOrderId(), e.getMessage());

            throw IntegrationException.erpIntegrationFailed(
                    order.getOrderId(), e.getMessage());
        }
    }

    /**
     * 물류 시스템으로 배송 정보 전송
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ExternalIntegrationLog sendOrderToLogistics(Order order) {
        log.info("물류 시스템으로 주문 전송 시작 - OrderId: {}", order.getOrderId());

        ExternalIntegrationLog log = ExternalIntegrationLog.create(
                order.getOrderId(),
                IntegrationType.LOGISTICS,
                "물류 시스템 배송 정보 전송 시도"
        );

        try {
            sendToExternalSystem(order);

            log.markSuccess("물류 시스템 전송 성공");
            integrationLogRepository.save(log);

            this.log.info("물류 시스템 전송 성공 - OrderId: {}", order.getOrderId());
            return log;

        } catch (Exception e) {
            log.incrementRetry();
            log.markFailure("물류 시스템 전송 실패: " + e.getMessage());
            integrationLogRepository.save(log);

            this.log.error("물류 시스템 전송 실패 - OrderId: {}", order.getOrderId());

            throw IntegrationException.logisticsIntegrationFailed(
                    order.getOrderId(), e.getMessage());
        }
    }

    /**
     * 외부 시스템 API 호출 시뮬레이션
     */
    private void sendToExternalSystem(Order order) {
        if (isSimulation) {
            // 시뮬레이션: 10% 확률로 실패
            if (Math.random() < 0.1) {
                throw new RuntimeException("외부 시스템 일시적 장애");
            }

            // 시뮬레이션: 네트워크 지연
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        log.debug("send order to external system!");
    }

    /**
     * 재시도 가능한 실패 로그 조회
     */
    public void retryFailedIntegrations(int maxRetryCount) {
        var failedLogs = integrationLogRepository.findFailedLogsForRetry(maxRetryCount);

        for (ExternalIntegrationLog failedLog : failedLogs) {
            log.info("재시도 대상 - OrderId: {}, Type: {}, RetryCount: {}",
                    failedLog.getOrderId(),
                    failedLog.getIntegrationType(),
                    failedLog.getRetryCount());

            // 재시도 로직
        }
    }

    public void setSimulation(boolean simulation) {
        this.isSimulation = simulation;
    }
}
