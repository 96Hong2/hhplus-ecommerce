package hhplus.ecommerce.common.presentation.response;

import lombok.Getter;

/**
 * 통일된 API 응답 형식을 제공하는 래퍼 클래스
 * 성공/실패 여부, 데이터, 메시지, 에러코드를 포함
 */
@Getter
public class ApiResponse<T> {
    private final boolean success;
    private final T data;
    private final String message;
    private final String errorCode;

    public ApiResponse(boolean success, T data, String message, String errorCode) {
        this.success = success;
        this.data = data;
        this.message = message;
        this.errorCode = errorCode;
    }

    // 성공 응답 - 데이터만
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, data, null, null);
    }

    // 성공 응답 - 데이터 + 메시지
    public static <T> ApiResponse<T> success(T data, String message) {
        return new ApiResponse<>(true, data, message, null);
    }

    // 실패 응답 - 메시지만
    public static <T> ApiResponse<T> error(String message) {
        return new ApiResponse<>(false, null, message, null);
    }

    // 실패 응답 - 메시지 + 에러코드
    public static <T> ApiResponse<T> error(String message, String errorCode) {
        return new ApiResponse<>(false, null, message, errorCode);
    }
}
