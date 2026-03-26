package jabaclass.user.auth.application.exception;

import org.springframework.http.HttpStatus;

import jabaclass.user.common.error.ErrorCode;

public enum AuthErrorCode implements ErrorCode {

	;

	private final HttpStatus status;
	private final String message;

	AuthErrorCode(HttpStatus status, String message) {
		this.status = status;
		this.message = message;
	}

	@Override
	public HttpStatus getStatus() {
		return status;
	}

	@Override
	public String getMessage() {
		return message;
	}
}