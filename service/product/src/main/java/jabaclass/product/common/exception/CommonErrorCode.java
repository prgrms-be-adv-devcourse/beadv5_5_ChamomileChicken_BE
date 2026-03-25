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
	SELLER_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 판매자 입니다."),
	// 403 판매자 권한이 없음
	NOT_SELLER(HttpStatus.FORBIDDEN, "판매자가 아닙니다."),
	// 404 룰 이름이 바뀐 경우
	INVALID_ROLE(HttpStatus.BAD_REQUEST, "룰 확인이 필요합니다. 관리자에게 문의 주세요."),
	// 400 상품명 확인 필요
	NOT_TITLE(HttpStatus.BAD_REQUEST, "상품명을 입력 해주세요."),
	// 400 유저 아이디 확인 필요
	NOT_USERID(HttpStatus.BAD_REQUEST, "판매자 Id를 입력 해주세요."),
	// 400 가격 확인 필요
	NOT_PRICE(HttpStatus.BAD_REQUEST, "가격은 1원 이상이어야 합니다."),
	// 400 총인원수 확인 필요
	NOT_MAXCAPACITY(HttpStatus.BAD_REQUEST, "예약 가능 인원수를 입력 해주세요."),
	// 400 상품 ID 확인 필요
	NOT_PRODUCTID(HttpStatus.BAD_REQUEST, "프로젝트 ID 입력 해주세요."),
	//500 삭제 실패
	FAIL_DELETE(HttpStatus.INTERNAL_SERVER_ERROR, "삭제 에러입니다. 서버 팀에 연락주세요!"),
	//404 판매자와 상품의 매치 실패
	MATCH_FAIL(HttpStatus.FORBIDDEN, "해당 상품은 본인 상품이 아닙니다."),
	// 401 인증 되지 않은 유저->sellerId가 비어있을 경우
	EMPTY_USER(HttpStatus.UNAUTHORIZED, "로그인 후 다시 이용해주세요.");

	private final HttpStatus status;
	private final String message;

	CommonErrorCode(HttpStatus status, String message) {
		this.status = status;
		this.message = message;
	}

	public static String from(CommonErrorCode code) {
		return code.getMessage();
	}

}
