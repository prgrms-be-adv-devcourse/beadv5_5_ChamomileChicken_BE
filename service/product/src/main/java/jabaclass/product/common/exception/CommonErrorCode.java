package jabaclass.product.common.exception;

import org.springframework.http.HttpStatus;

import lombok.Getter;

@Getter
public enum CommonErrorCode {
	//400 BAD_REQUEST 잘못된 요청
	INVALID_PARAMETER(HttpStatus.BAD_REQUEST, "파라미터 값을 확인해주세요."),
	//500 INTERNAL SERVER ERROR
	INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 에러입니다. 서버 팀에 연락주세요!"),
	//404 NOT_FOUND 잘못된 리소스 접근
	PRODUCT_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 상품 ID 입니다."),
	// 404 판매자 없음
	SELLER_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 판매자입니다."),
	// 403 판매자 권한이 없음
	NOT_SELLER(HttpStatus.FORBIDDEN, "판매자가 아닙니다.");

	private final HttpStatus status;
	private final String message;

	CommonErrorCode(HttpStatus status, String message) {
		this.status = status;
		this.message = message;
	}

}
