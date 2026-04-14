package jabaclass.payment.common.error;

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

	public PaymentException(PaymentErrorCode errorCode, Throwable cause) {
		super(errorCode.getMessage(), cause);
		this.errorCode = errorCode;
	}
}
