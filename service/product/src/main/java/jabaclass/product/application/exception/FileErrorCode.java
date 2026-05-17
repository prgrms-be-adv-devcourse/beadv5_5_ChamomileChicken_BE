package jabaclass.product.application.exception;

import org.springframework.http.HttpStatus;

public enum FileErrorCode {

    FILE_NOT_FOUND(HttpStatus.NOT_FOUND, "파일을 찾을 수 없습니다."),
    FILE_NOT_UPLOADED(HttpStatus.BAD_REQUEST, "S3에 파일이 존재하지 않습니다."),
    FILE_ALREADY_CONFIRMED(HttpStatus.BAD_REQUEST, "이미 처리된 파일입니다."),
    FILE_INVALID_TYPE(HttpStatus.BAD_REQUEST, "허용되지 않는 파일 형식입니다."),
    FILE_SIZE_EXCEEDED(HttpStatus.BAD_REQUEST, "파일 크기가 제한을 초과했습니다.");

    private final HttpStatus status;
    private final String message;

    FileErrorCode(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }
}
