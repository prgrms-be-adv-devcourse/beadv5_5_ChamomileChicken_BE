package jabaclass.product.application.exception;

import org.springframework.http.HttpStatus;

import jabaclass.product.common.exception.CommonErrorCode;

public class BusinessException extends RuntimeException {
	private final CommonErrorCode errorCode;

	public BusinessException(CommonErrorCode errorCode) {
		super(errorCode.getMessage());
		this.errorCode = errorCode;
	}

	public HttpStatus getStatus() {
		return errorCode.getStatus();
	}

	public String getMessage() {
		return errorCode.getMessage();
	}

}
