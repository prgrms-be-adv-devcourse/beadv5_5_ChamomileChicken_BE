package jabaclass.file.common.exception;

import org.springframework.http.HttpStatus;

public class FileException extends RuntimeException {

    private final FileErrorCode errorCode;

    public FileException(FileErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public FileErrorCode getErrorCode() {
        return errorCode;
    }

    public HttpStatus getStatus() {
        return errorCode.getStatus();
    }
}
