package jabaclass.file.common.exception;

import jabaclass.file.common.dto.ApiResponseDto;
import jabaclass.auth.exception.JwtAuthException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.http.HttpStatus;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponseDto<Void>> handleValidationException(
            MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult()
                .getFieldError()
                .getDefaultMessage();
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponseDto.fail(HttpStatus.BAD_REQUEST, message));
    }

    @ExceptionHandler(FileException.class)
    public ResponseEntity<ApiResponseDto<Void>> handleFileException(FileException ex) {
        return ResponseEntity
                .status(ex.getStatus())
                .body(ApiResponseDto.fail(ex.getStatus(), ex.getMessage()));
    }

    @ExceptionHandler(JwtAuthException.class)
    public ResponseEntity<ApiResponseDto<Void>> handleJwtAuthException(JwtAuthException ex) {
        return ResponseEntity
                .status(ex.getErrorCode().getStatus())
                .body(ApiResponseDto.fail(ex.getErrorCode().getStatus(), ex.getErrorCode().getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponseDto<Void>> handleServerError(Exception ex) {
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponseDto.fail(HttpStatus.INTERNAL_SERVER_ERROR, "서버 에러입니다."));
    }
}
