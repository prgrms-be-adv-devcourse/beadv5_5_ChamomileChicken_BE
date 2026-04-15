package jabaclass.settlement.application.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import jakarta.validation.ConstraintViolationException;

@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ApiResponseDto<Void>> handleValidationException(MethodArgumentNotValidException ex) {
		FieldError fieldError = ex.getBindingResult().getFieldError();
		String message = fieldError != null ? fieldError.getDefaultMessage() : CommonErrorCode.INVALID_PARAMETER.getMessage();

		return ResponseEntity.status(HttpStatus.BAD_REQUEST)
			.body(ApiResponseDto.fail(HttpStatus.BAD_REQUEST, message));
	}

	@ExceptionHandler(ConstraintViolationException.class)
	public ResponseEntity<ApiResponseDto<Void>> handleConstraintViolationException(ConstraintViolationException ex) {
		return ResponseEntity.status(HttpStatus.BAD_REQUEST)
			.body(ApiResponseDto.fail(HttpStatus.BAD_REQUEST, ex.getMessage()));
	}

	@ExceptionHandler(IllegalArgumentException.class)
	public ResponseEntity<ApiResponseDto<Void>> handleIllegalArgumentException(IllegalArgumentException ex) {
		return ResponseEntity.status(HttpStatus.BAD_REQUEST)
			.body(ApiResponseDto.fail(HttpStatus.BAD_REQUEST, ex.getMessage()));
	}

	@ExceptionHandler(BusinessException.class)
	public ResponseEntity<ApiResponseDto<Void>> handleBusinessException(BusinessException ex) {
		return ResponseEntity.status(ex.getStatus())
			.body(ApiResponseDto.fail(ex.getStatus(), ex.getMessage()));
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ApiResponseDto<Void>> handleServerError(Exception ex) {
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
			.body(ApiResponseDto.fail(
				CommonErrorCode.INTERNAL_SERVER_ERROR.getStatus(),
				CommonErrorCode.INTERNAL_SERVER_ERROR.getMessage()
			));
	}
}
