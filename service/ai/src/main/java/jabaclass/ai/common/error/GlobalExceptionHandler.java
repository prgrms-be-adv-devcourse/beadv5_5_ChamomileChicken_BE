package jabaclass.ai.common.error;

import lombok.extern.slf4j.Slf4j;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ApiResponseDto<Void>> handleValidationException(
		MethodArgumentNotValidException ex,
		HttpServletRequest request
	) {
		String message = ex.getBindingResult().getFieldErrors().stream()
			.map(fieldError -> fieldError.getDefaultMessage())
			.collect(java.util.stream.Collectors.joining(", "));
		log.warn("Validation Error path={} method={} message={}",
			request.getRequestURI(), request.getMethod(), message);

		return ResponseEntity
			.status(HttpStatus.BAD_REQUEST)
			.body(ApiResponseDto.fail(HttpStatus.BAD_REQUEST, message));
	}

	@ExceptionHandler(BusinessException.class)
	public ResponseEntity<ApiResponseDto<Void>> handleBusinessException(
		BusinessException ex,
		HttpServletRequest request
	) {
		log.warn("Business Error path={} method={} type={} message={}",
			request.getRequestURI(), request.getMethod(), ex.getClass().getSimpleName(), ex.getMessage());
		return ResponseEntity
			.status(ex.getStatus())
			.body(ApiResponseDto.fail(ex.getStatus(), ex.getMessage()));
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ApiResponseDto<Void>> handleServerError(Exception ex, HttpServletRequest request) {
		log.error("Internal Server Error path={} method={} type={} message={}",
			request.getRequestURI(),
			request.getMethod(),
			ex.getClass().getName(),
			ex.getMessage(),
			ex);
		return ResponseEntity
			.status(HttpStatus.INTERNAL_SERVER_ERROR)
			.body(ApiResponseDto.fail(
				CommonErrorCode.INTERNAL_SERVER_ERROR.getStatus(),
				CommonErrorCode.INTERNAL_SERVER_ERROR.getMessage()
			));
	}
}
