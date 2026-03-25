package jabaclass.user.mail.application.exception;

import org.springframework.http.HttpStatus;

import jabaclass.user.common.error.ErrorCode;

public enum MailErrorCode implements ErrorCode {

	MAIL_SEND_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "메일 전송에 실패했습니다.");

	private final HttpStatus status;
	private final String message;

	MailErrorCode(HttpStatus status, String message) {
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