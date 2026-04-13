package jabaclass.order.application.exception;

import org.springframework.http.HttpStatus;

import jabaclass.order.common.error.ErrorCode;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum OrderErrorCode implements ErrorCode {

    ORDER_NOT_FOUND(HttpStatus.NOT_FOUND, "주문을 찾을 수 없습니다."),
    ORDER_ACCESS_DENIED(HttpStatus.FORBIDDEN, "본인의 주문만 조회 및 취소할 수 있습니다."),
    ORDER_CANCEL_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "현재 주문 상태에서는 취소할 수 없습니다."),
    ORDER_DEPOSIT_NOT_AVAILABLE(HttpStatus.BAD_REQUEST, "사용 가능한 예치금이 부족합니다."),
    ORDER_PRODUCT_NOT_AVAILABLE(HttpStatus.BAD_REQUEST, "예약 가능한 상품 일정이 아닙니다."),
    ORDER_QUANTITY_INVALID(HttpStatus.BAD_REQUEST, "수량은 1 이상이어야 합니다."),
    ORDER_PRODUCT_USER_ID_REQUIRED(HttpStatus.BAD_REQUEST, "상품 소유자 ID는 비어 있을 수 없습니다."),
    ORDER_PRICE_INVALID(HttpStatus.BAD_REQUEST, "주문 가격은 0 이상이어야 합니다."),
    ORDER_DEPOSIT_AMOUNT_INVALID(HttpStatus.BAD_REQUEST, "예치금 사용 금액은 0 이상이어야 합니다."),
    ORDER_DEPOSIT_AMOUNT_EXCEEDS_PRICE(HttpStatus.BAD_REQUEST, "예치금 사용 금액은 주문 가격보다 클 수 없습니다."),
    ORDER_PRODUCT_PRICE_INVALID(HttpStatus.BAD_REQUEST, "상품 가격은 0 이상이어야 합니다."),
    ORDER_EXPIRE_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "PENDING 상태만 만료 가능합니다."),
    ORDER_PAY_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "PENDING 상태만 결제 완료 처리할 수 있습니다."),
    ORDER_FAIL_PAYMENT_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "PENDING 상태만 결제 실패 처리할 수 있습니다.");

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
