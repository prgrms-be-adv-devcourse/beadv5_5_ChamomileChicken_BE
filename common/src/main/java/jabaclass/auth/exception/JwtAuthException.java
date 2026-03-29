package jabaclass.auth.exception;

import org.springframework.http.HttpStatus;

public class JwtAuthException extends RuntimeException {

    private final JwtErrorCode errorCode;

    public JwtAuthException(JwtErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public JwtErrorCode getErrorCode() {
        return errorCode;
    }

    public HttpStatus getStatus() {
        return errorCode.getStatus();
    }
}