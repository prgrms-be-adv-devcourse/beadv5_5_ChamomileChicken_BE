package jabaclass.order.order.application.exception;

import org.springframework.http.HttpStatus;

import jabaclass.order.common.error.ErrorCode;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum OrderErrorCode implements ErrorCode {

    ORDER_NOT_FOUND(HttpStatus.NOT_FOUND, "주문을 찾을 수 없습니다."),
    ORDER_ACCESS_DENIED(HttpStatus.FORBIDDEN, "본인의 주문만 조회 및 취소할 수 있습니다."),
    ORDER_CANCEL_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "현재 주문 상태에서는 취소할 수 없습니다."),
    ORDER_DEPOSIT_NOT_AVAILABLE(HttpStatus.BAD_REQUEST, "사용 가능한 예치금이 부족합니다."),
    ORDER_PRODUCT_NOT_AVAILABLE(HttpStatus.BAD_REQUEST, "예약 가능한 상품 일정이 아닙니다.");

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
