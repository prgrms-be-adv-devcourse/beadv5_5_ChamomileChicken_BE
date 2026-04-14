package jabaclass.payment.common.error;

import org.springframework.http.HttpStatus;

public enum PaymentErrorCode {

	PAYMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "결제 정보를 찾을 수 없습니다."),
	INVALID_ORDER_AMOUNT(HttpStatus.BAD_REQUEST, "주문 금액이 일치하지 않습니다."),
	INVALID_PAYMENT_AMOUNT(HttpStatus.BAD_REQUEST, "결제 금액이 일치하지 않습니다."),
	PAYMENT_CONFIRM_FAILED(HttpStatus.BAD_GATEWAY, "결제 승인에 실패했습니다."),
	PAYMENT_REFUND_FAILED(HttpStatus.BAD_GATEWAY, "환불 처리에 실패했습니다."),
	PAYMENT_ALREADY_COMPLETED(HttpStatus.BAD_REQUEST, "이미 완료된 결제입니다."),
	PAYMENT_NOT_COMPLETED(HttpStatus.BAD_REQUEST, "완료된 결제만 처리할 수 있습니다."),
	PAYMENT_INVALID_STATUS(HttpStatus.BAD_REQUEST, "유효하지 않은 결제 상태입니다."),
	INVALID_AMOUNT(HttpStatus.BAD_REQUEST, "금액은 null일 수 없습니다."),
	INVALID_PRODUCT_USER(HttpStatus.BAD_REQUEST, "productUserId는 null일 수 없습니다."),
	INVALID_NEGATIVE_AMOUNT(HttpStatus.BAD_REQUEST, "금액은 0 이상이어야 합니다."),
	DEPOSIT_PAYMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "예치금 결제 정보를 찾을 수 없습니다."),
	DEPOSIT_PAYMENT_CONFIRM_FAILED(HttpStatus.BAD_GATEWAY, "예치금 결제 승인에 실패했습니다."),
	ORDER_VALIDATION_FAILED(HttpStatus.BAD_GATEWAY, "Order 검증에 실패했습니다."),
	ORDER_RESPONSE_INVALID(HttpStatus.BAD_GATEWAY, "Order 응답이 올바르지 않습니다."),
	ORDER_UPDATE_FAILED(HttpStatus.BAD_GATEWAY, "Order 상태 업데이트에 실패했습니다."),
	USER_DEPOSIT_INCREASE_FAILED(HttpStatus.BAD_GATEWAY, "유저 예치금 증가에 실패했습니다."),
	USER_DEPOSIT_REFUND_FAILED(HttpStatus.BAD_GATEWAY, "유저 예치금 환불에 실패했습니다."),
	UNAUTHORIZED_PAYMENT_ACCESS(HttpStatus.FORBIDDEN, "본인의 결제가 아닙니다.");

	private final HttpStatus status;
	private final String message;

	PaymentErrorCode(HttpStatus status, String message) {
		this.status = status;
		this.message = message;
	}

	public HttpStatus getStatus() {
		return status;
	}

	public String getMessage() {
		return message;
	}
}
