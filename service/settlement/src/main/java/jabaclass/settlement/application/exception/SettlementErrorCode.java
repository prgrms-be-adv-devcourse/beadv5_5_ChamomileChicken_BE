package jabaclass.settlement.application.exception;

import org.springframework.http.HttpStatus;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum SettlementErrorCode implements ErrorCode {

	SETTLEMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "정산 정보를 찾을 수 없습니다."),
	SETTLEMENT_PROMOTION_NOT_FOUND(HttpStatus.INTERNAL_SERVER_ERROR, "정산 프로모션 정보를 찾을 수 없습니다."),
	SELLER_GRADE_POLICY_NOT_FOUND(HttpStatus.INTERNAL_SERVER_ERROR, "판매자 등급 정책을 찾을 수 없습니다."),
	INVALID_SETTLEMENT_STATUS(HttpStatus.BAD_REQUEST, "정산 상태가 올바르지 않습니다."),
	INVALID_SETTLEMENT_AMOUNT(HttpStatus.BAD_REQUEST, "정산 금액이 올바르지 않습니다."),
	SETTLEMENT_TRANSFER_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "송금이 불가능한 정산입니다."),
	SETTLEMENT_ACCOUNT_NOT_FOUND(HttpStatus.BAD_REQUEST, "판매자 정산 계좌 정보가 없습니다."),
	SETTLEMENT_ACCOUNT_NOT_ACTIVE(HttpStatus.BAD_REQUEST, "판매자 정산 계좌가 비활성 상태입니다.");

	private final HttpStatus status;
	private final String message;

	@Override
	public HttpStatus getStatus() {
		return status;
	}

	@Override
	public String getMessage() {
		return message;
	}
}
