package jabaclass.user.deposit.domain.exception;

import org.springframework.http.HttpStatus;

import jabaclass.user.common.error.ErrorCode;

public enum DepositErrorCode implements ErrorCode {

	NOT_FOUND_USER(HttpStatus.NOT_FOUND, "존재하지 않는 회원입니다."),
	NOT_FOUND_DEPOSIT_HISTORY(HttpStatus.NOT_FOUND, "존재하지 않는 예치금 이력입니다."),
	PAYMENT_SERVICE_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "결제 서비스에 연결할 수 없습니다."),
	PAYMENT_FAILED(HttpStatus.BAD_REQUEST, "결제에 실패했습니다."),
	INSUFFICIENT_DEPOSIT(HttpStatus.BAD_REQUEST, "예치금이 부족합니다.");

	private final HttpStatus status;
	private final String message;

	DepositErrorCode(HttpStatus status, String message) {
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