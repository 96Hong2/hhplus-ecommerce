package hhplus.ecommerce.common.domain.exception;

import hhplus.ecommerce.common.domain.constants.ErrorCode;

public class UserException extends BusinessException {
    
    private UserException(String errorCode, String message) {
        super(errorCode, message);
    }

    private UserException(String errorCode, String message, Throwable cause) {
        super(errorCode, message, cause);
    }
    
    public static UserException userNotFound(Long userId) {
        String message = String.format(
                "존재하지 않는 사용자입니다. [사용자ID: %d]", userId
        );
        
        return new UserException(ErrorCode.USER_NOT_FOUND, message);
    }
    
    public static UserException creationFailed(String reason) {
        String message = String.format(
                "사용자 등록에 실패하였습니다. [사유: %s]", reason
        );
        
        return new UserException(ErrorCode.USER_CREATION_FAILED, message);
    }

    public static UserException getUserListFailed(String reason) {
        String message = String.format(
                "사용자 목록 조회에 실패하였습니다. [사유: %s]",
                reason
        );

        return new UserException(ErrorCode.USER_GET_LIST_FAILED, message);
    }
    
    public static UserException authenticationFailed(Long userId, String reason) {
        String message = String.format(
                "사용자 인증에 실패하였습니다. [사용자ID: %d, 사유: %s]",
                userId, reason
        );

        return new UserException(ErrorCode.USER_AUTHENTICATION_FAILED, message);
    }

    public static UserException authorizationFailed(Long userId, String reason) {
        String message = String.format(
                "권한이 없는 사용자입니다. [사용자ID: %d, 사유: %s]",
                userId, reason
        );

        return new UserException(ErrorCode.USER_AUTHORIZATION_FAILED, message);
    }
}
