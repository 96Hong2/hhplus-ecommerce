package hhplus.ecommerce.common.presentation.exception;

import hhplus.ecommerce.common.domain.exception.BusinessException;
import hhplus.ecommerce.common.presentation.response.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 전역 예외 처리 핸들러
 * 비즈니스 예외를 HTTP 상태코드와 함께 ApiResponse 형식으로 변환
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 비즈니스 예외 처리
     * 도메인별 예외를 400 Bad Request로 반환
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException e) {
        ApiResponse<Void> response = ApiResponse.error(e.getMessage(), e.getErrorCode());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(response);
    }

    /**
     * Validation 예외 처리
     * @Valid 검증 실패 시 첫 번째 에러 메시지 반환
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult()
                .getFieldErrors()
                .stream()
                .findFirst()
                .map(error -> error.getField() + " : " + error.getDefaultMessage())
                .orElse("유효하지 않은 요청입니다.");

        ApiResponse<Void> response = ApiResponse.error(message);
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(response);
    }

    /**
     * 예상치 못한 서버 예외 처리
     * 500 Internal Server Error 반환
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(Exception e) {
        ApiResponse<Void> response = ApiResponse.error("서버 내부 오류가 발생했습니다. : " + e.getMessage());
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(response);
    }
}