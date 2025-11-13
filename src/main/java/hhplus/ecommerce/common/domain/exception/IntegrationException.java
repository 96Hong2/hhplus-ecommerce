package hhplus.ecommerce.common.domain.exception;

import hhplus.ecommerce.common.domain.constants.ErrorCode;

public class IntegrationException extends BusinessException {

    private IntegrationException(String errorCode, String message) {
        super(errorCode, message);
    }

    private IntegrationException(String errorCode, String message, Throwable cause) {
        super(errorCode, message, cause);
    }

    public static IntegrationException integrationFailed(String integrationType, String reason) {
        String message = String.format("외부 시스템 연동에 실패했습니다. [시스템: %s, 사유: %s]",
                integrationType, reason);
        return new IntegrationException(ErrorCode.INTEGRATION_FAILED, message);
    }

    public static IntegrationException integrationLogNotFound(Long logId) {
        String message = String.format("연동 로그를 찾을 수 없습니다. [로그ID: %d]", logId);
        return new IntegrationException(ErrorCode.INTEGRATION_LOG_NOT_FOUND, message);
    }

    public static IntegrationException logisticsIntegrationFailed(Long orderId, String reason) {
        String message = String.format("물류 시스템 연동에 실패했습니다. [주문ID: %d, 사유: %s]",
                orderId, reason);
        return new IntegrationException(ErrorCode.LOGISTICS_INTEGRATION_FAILED, message);
    }

    public static IntegrationException salesManagementIntegrationFailed(Long orderId, String reason) {
        String message = String.format("매출관리 시스템 연동에 실패했습니다. [주문ID: %d, 사유: %s]",
                orderId, reason);
        return new IntegrationException(ErrorCode.SALES_MANAGEMENT_INTEGRATION_FAILED, message);
    }

    public static IntegrationException erpIntegrationFailed(Long orderId, String reason) {
        String message = String.format("ERP 시스템 연동에 실패했습니다. [주문ID: %d, 사유: %s]",
                orderId, reason);
        return new IntegrationException(ErrorCode.ERP_INTEGRATION_FAILED, message);
    }

    public static IntegrationException integrationRetryFailed(Long logId, int retryCount) {
        String message = String.format("연동 재시도에 실패했습니다. [로그ID: %d, 재시도횟수: %d]",
                logId, retryCount);
        return new IntegrationException(ErrorCode.INTEGRATION_RETRY_FAILED, message);
    }

    public static IntegrationException integrationMaxRetryExceeded(Long logId, int maxRetry) {
        String message = String.format("최대 재시도 횟수를 초과했습니다. [로그ID: %d, 최대재시도: %d]",
                logId, maxRetry);
        return new IntegrationException(ErrorCode.INTEGRATION_MAX_RETRY_EXCEEDED, message);
    }

    public static IntegrationException invalidIntegrationType(String integrationType) {
        String message = String.format("유효하지 않은 연동 시스템 타입입니다. [타입: %s]", integrationType);
        return new IntegrationException(ErrorCode.INVALID_INTEGRATION_TYPE, message);
    }
}
