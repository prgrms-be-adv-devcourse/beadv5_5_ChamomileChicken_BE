package jabaclass.payment.common.exception;

import org.springframework.http.HttpStatus;

public class PaymentException extends RuntimeException {

	private final PaymentErrorCode errorCode;

	public PaymentException(PaymentErrorCode errorCode) {
		super(errorCode.getMessage());
		this.errorCode = errorCode;
	}

	public PaymentErrorCode getErrorCode() {
		return errorCode;
	}

	public HttpStatus getStatus() {
		return errorCode.getStatus();
	}
}
