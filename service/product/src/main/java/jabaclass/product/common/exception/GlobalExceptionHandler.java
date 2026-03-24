package jabaclass.product.common.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import jabaclass.product.application.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

	// Validation 에러 처리
	// request 값의 문제이므로 HttpStatus.BAD_REQUEST로 고정했습니다.
	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ApiResponseDto<Void>> handleValidationException(MethodArgumentNotValidException ex) {
		
		String message = ex.getBindingResult().getFieldErrors().stream()
			.map(fieldError -> fieldError.getDefaultMessage())
			.collect(java.util.stream.Collectors.joining(", "));

		return ResponseEntity
			.status(HttpStatus.BAD_REQUEST)
			.body(ApiResponseDto.fail(HttpStatus.BAD_REQUEST, message));
	}

	// BusinessException 처리
	// 서비스 로직의 예외 처리 입니다.
	@ExceptionHandler(BusinessException.class)
	public ResponseEntity<ApiResponseDto<Void>> handleBusinessException(BusinessException ex) {
		return ResponseEntity
			.status(ex.getStatus())
			.body(ApiResponseDto.fail(ex.getStatus(), ex.getMessage()));
	}

	// 서버 에러 처리
	// 500 에러에 대한 처리로, 500 고정 해두었습니다
	@ExceptionHandler(Exception.class)
	public ResponseEntity<ApiResponseDto<Void>> handleServerError(Exception ex) {
		log.error("Internal Server Error", ex); // 예외 스택 트레이스 로깅 추가
		return ResponseEntity
			.status(HttpStatus.INTERNAL_SERVER_ERROR)
			.body(ApiResponseDto.fail(CommonErrorCode.INTERNAL_SERVER_ERROR.getStatus()
				, CommonErrorCode.INTERNAL_SERVER_ERROR.getMessage()));
	}
}
