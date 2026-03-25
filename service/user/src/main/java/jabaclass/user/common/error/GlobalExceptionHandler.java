package jabaclass.user.common.error;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import jabaclass.auth.exception.JwtAuthException;
import jabaclass.user.common.dto.ApiResponseDto;

@RestControllerAdvice
public class GlobalExceptionHandler {

	/**
	 * Validation 에러 처리
	 * Request 값의 문제이므로 HttpStatus.BAD_REQUEST로 고정합니다.
	 */
	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ApiResponseDto<Void>> handleValidationException(MethodArgumentNotValidException ex) {
		String message = ex.getBindingResult()
				.getFieldError()
				.getDefaultMessage();

		return ResponseEntity.status(HttpStatus.BAD_REQUEST)
				.body(ApiResponseDto.fail(HttpStatus.BAD_REQUEST, message));
	}

	/**
	 * BusinessException 처리
	 * 서비스 로직에서 발생하는 비즈니스 예외를 처리합니다.
	 */
	@ExceptionHandler(BusinessException.class)
	public ResponseEntity<ApiResponseDto<Void>> handleBusinessException(BusinessException ex) {
		return ResponseEntity.status(ex.getStatus())
				.body(ApiResponseDto.fail(ex.getStatus(), ex.getMessage()));
	}

	/**
	 * JwtAuthException 처리
	 * JWT 인증 관련 예외를 처리합니다.
	 */
	@ExceptionHandler(JwtAuthException.class)
	public ResponseEntity<ApiResponseDto<Void>> handleJwtAuthException(JwtAuthException ex) {
		return ResponseEntity.status(ex.getErrorCode().getStatus())
				.body(ApiResponseDto.fail(ex.getErrorCode().getStatus(), ex.getErrorCode().getMessage()));
	}

	/**
	 * 서버 에러 처리
	 * 최상위 예외인 Exception에 대해 500 에러로 응답합니다.
	 */
	@ExceptionHandler(Exception.class)
	public ResponseEntity<ApiResponseDto<Void>> handleServerError(Exception ex) {
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
				.body(ApiResponseDto.fail(
						CommonErrorCode.INTERNAL_SERVER_ERROR.getStatus(),
						CommonErrorCode.INTERNAL_SERVER_ERROR.getMessage()
				));
	}
}