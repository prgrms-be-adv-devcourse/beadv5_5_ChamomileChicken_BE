package jabastore.auth.exception;

public class JwtAuthException extends RuntimeException {

    private final JwtErrorCode errorCode;

    public JwtAuthException(JwtErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public JwtErrorCode getErrorCode() {
        return errorCode;
    }

    public org.springframework.http.HttpStatus getStatus() {
        return errorCode.getStatus();
    }
}
