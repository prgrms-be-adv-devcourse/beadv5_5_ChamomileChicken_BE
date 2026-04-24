package jabaclass.admin.common.error;

import org.springframework.http.HttpStatus;

public enum AdminErrorCode implements ErrorCode {

	FORBIDDEN(HttpStatus.FORBIDDEN, "관리자 권한이 필요합니다."),
	USER_NOT_FOUND(HttpStatus.NOT_FOUND, "유저를 찾을 수 없습니다."),
	PRODUCT_NOT_FOUND(HttpStatus.NOT_FOUND, "상품을 찾을 수 없습니다."),
	REVIEW_NOT_FOUND(HttpStatus.NOT_FOUND, "리뷰를 찾을 수 없습니다."),
	ORDER_NOT_FOUND(HttpStatus.NOT_FOUND, "주문을 찾을 수 없습니다."),
	SETTLEMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "정산을 찾을 수 없습니다."),
	INVALID_SELLER_APPROVAL(HttpStatus.BAD_REQUEST, "일반 유저만 판매자로 승인할 수 있습니다."),
	INVALID_STATUS_VALUE(HttpStatus.BAD_REQUEST, "유효하지 않은 상태 값입니다."),
	INVALID_ROLE_VALUE(HttpStatus.BAD_REQUEST, "유효하지 않은 역할 값입니다."),
	OUTBOX_SERIALIZATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "이벤트 직렬화에 실패했습니다.");

	private final HttpStatus status;
	private final String message;

	AdminErrorCode(HttpStatus status, String message) {
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
