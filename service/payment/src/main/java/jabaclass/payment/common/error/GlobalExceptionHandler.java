package jabaclass.payment.common.error;

import jabaclass.payment.common.dto.ApiResponseDto;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(PaymentException.class)
	public ResponseEntity<ApiResponseDto<Void>> handlePaymentException(PaymentException ex) {

		PaymentErrorCode errorCode = ex.getErrorCode();

		log.warn("[PaymentException] code={}, message={}",
			errorCode.name(),
			errorCode.getMessage(),
			ex
		);

		return ResponseEntity
			.status(errorCode.getStatus())
			.body(ApiResponseDto.fail(
				errorCode.getStatus(),
				errorCode.getMessage()
			));
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ApiResponseDto<Void>> handleServerError(Exception ex) {

		log.error("[Unhandled Exception]", ex);

		return ResponseEntity
			.status(HttpStatus.INTERNAL_SERVER_ERROR)
			.body(ApiResponseDto.fail(HttpStatus.INTERNAL_SERVER_ERROR, "서버 에러입니다."));
	}
}
