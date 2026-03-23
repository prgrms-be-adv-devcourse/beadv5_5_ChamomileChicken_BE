package jabaclass.product.common.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import jabaclass.product.application.exception.BusinessException;

@RestControllerAdvice
public class GlobalExceptionHandler {

	// TODO 공통 예외처리에 대한 테스트, Service 예외 처리를 실무적으로 실행

	// Validation 에러 처리
	// request 값의 문제이므로 HttpStatus.BAD_REQUEST로 고정했습니다.
	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ApiResponseDto<Void>> handleValidationException(MethodArgumentNotValidException ex) {
		// 모든 필드 에러 메시지 중 첫 번째 가져오기
		String message = ex.getBindingResult()
			.getFieldError()  // 첫 번째 에러
			.getDefaultMessage(); // DTO에 설정한 메시지

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
		return ResponseEntity
			.status(HttpStatus.INTERNAL_SERVER_ERROR)
			.body(ApiResponseDto.fail(CommonErrorCode.INTERNAL_SERVER_ERROR.getStatus()
				, CommonErrorCode.INTERNAL_SERVER_ERROR.getMessage()));
	}
}
