package jabaclass.order.application.port.external;

import java.math.BigDecimal;
import java.util.UUID;

import jabaclass.order.infrastructure.client.payment.dto.InternalRefundResponseDto;

import jabaclass.order.infrastructure.client.payment.dto.PaymentRefundInfoResponseDto;

public interface PaymentPort {

    InternalRefundResponseDto refund(UUID orderId, BigDecimal refundRate);

	PaymentRefundInfoResponseDto getRefundInfo(UUID orderId);
}
