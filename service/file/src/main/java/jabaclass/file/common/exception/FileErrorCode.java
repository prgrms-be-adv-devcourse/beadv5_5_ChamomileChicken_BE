package jabaclass.file.common.exception;

import org.springframework.http.HttpStatus;

public enum FileErrorCode {

    FILE_NOT_FOUND(HttpStatus.NOT_FOUND, "파일을 찾을 수 없습니다."),
    FILE_NOT_UPLOADED(HttpStatus.BAD_REQUEST, "S3에 파일이 존재하지 않습니다."),
    FILE_ALREADY_CONFIRMED(HttpStatus.BAD_REQUEST, "이미 처리된 파일입니다.");

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
