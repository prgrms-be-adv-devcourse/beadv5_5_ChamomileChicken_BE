package jabaclass.apigateway.exception;

import org.springframework.http.HttpStatus;

public enum SystemErrorCode implements ErrorCode {
    AUTH_SERVICE_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "일시적인 서버 오류입니다. 잠시 후 다시 시도해주세요."),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "일시적인 내부 서버 오류입니다. 잠시 후 다시 시도해주세요.");

    private final HttpStatus status;
    private final String message;

    SystemErrorCode(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }

    @Override
    public HttpStatus getStatus() {
        return status;
    }

    @Override
    public String getMessage() {
        return message;
    }
}
