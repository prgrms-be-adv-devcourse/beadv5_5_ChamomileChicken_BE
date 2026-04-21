package jabaclass.product.presentation.dto.response;

// 2026-04-09 product 검증 추가
public enum OrderValid {
	// 가격이 맞지 않는 경우 return
	PRICE_MISMATCH,
	// 재고 부족일 경우 return
	OUT_OF_STOCK,
	// 재고 차감 완료
	OK,
	// 수정 실패
	MODI_FAIL
}
