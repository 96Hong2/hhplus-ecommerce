package hhplus.ecommerce.common.domain.exception;

/**
 * 비즈니스 예외의 최상위 추상 클래스
 * 모든 도메인 예외는 이 클래스를 상속받아서 에러 코드를 포함함
 */
public class BusinessException extends RuntimeException {

    private final String errorCode;

    protected BusinessException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    protected BusinessException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
