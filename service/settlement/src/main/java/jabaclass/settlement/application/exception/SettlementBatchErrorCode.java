package jabaclass.settlement.application.exception;

import org.springframework.http.HttpStatus;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum SettlementBatchErrorCode implements ErrorCode {

	SETTLEMENT_BATCH_ALREADY_RUNNING(HttpStatus.CONFLICT, "이미 실행 중인 정산 배치입니다."),
	SETTLEMENT_CALCULATE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "정산 계산 배치 실행에 실패했습니다."),
	SETTLEMENT_TRANSFER_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "정산 송금 배치 실행에 실패했습니다."),
	SETTLEMENT_BATCH_PARAMETER_INVALID(HttpStatus.BAD_REQUEST, "배치 실행 파라미터가 올바르지 않습니다.");

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
