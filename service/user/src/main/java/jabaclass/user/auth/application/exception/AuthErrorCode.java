package jabaclass.user.auth.application.exception;

import org.springframework.http.HttpStatus;

import jabaclass.user.common.error.ErrorCode;

public enum AuthErrorCode implements ErrorCode {

	USER_NOT_FOUND(HttpStatus.NOT_FOUND, "이메일 또는 비밀번호가 올바르지 않습니다."),
	INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "유효하지 않은 refresh token입니다."),
	REFRESH_TOKEN_MISMATCH(HttpStatus.UNAUTHORIZED, "refresh token이 일치하지 않습니다."),
	ALREADY_LOGGED_OUT(HttpStatus.UNAUTHORIZED, "이미 로그아웃된 유저입니다."),
	INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "유효하지 않은 인증 정보입니다."),
	INVALID_REQUEST(HttpStatus.UNAUTHORIZED, "인증 정보가 존재하지 않습니다."),
	INVALID_EMAIL(HttpStatus.CONFLICT, "가입할 수 없는 이메일입니다. 다른 이메일을 사용해주세요"),
	UNSUPPORTED_PROVIDER(HttpStatus.BAD_REQUEST, "지원하지 않는 소셜 로그인입니다.");

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
