package jabaclass.user.mail.application.exception;

import org.springframework.http.HttpStatus;

import jabaclass.user.common.error.ErrorCode;

public enum MailErrorCode implements ErrorCode {

	MAIL_SEND_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "메일 전송에 실패했습니다."),
	EMAIL_VERIFICATION_NOT_FOUND(HttpStatus.NOT_FOUND, "이메일 인증 정보가 존재하지 않습니다."),
	EMAIL_VERIFICATION_CODE_EXPIRED(HttpStatus.BAD_REQUEST, "인증코드가 만료되었습니다."),
	EMAIL_VERIFICATION_CODE_MISMATCH(HttpStatus.BAD_REQUEST, "인증코드가 올바르지 않습니다."),
	EMAIL_VERIFICATION_TOKEN_NOT_FOUND(HttpStatus.NOT_FOUND, "이메일 인증 토큰이 존재하지 않습니다."),
	EMAIL_VERIFICATION_TOKEN_EXPIRED(HttpStatus.BAD_REQUEST, "이메일 인증 토큰이 만료되었습니다."),
	EMAIL_VERIFICATION_TOKEN_MISMATCH(HttpStatus.BAD_REQUEST, "이메일 인증 토큰이 올바르지 않습니다.");

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