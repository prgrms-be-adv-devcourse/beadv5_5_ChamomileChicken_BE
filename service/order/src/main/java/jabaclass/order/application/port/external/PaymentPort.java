package jabaclass.order.application.port.external;

import java.math.BigDecimal;
import java.util.UUID;

import jabaclass.order.infrastructure.client.payment.dto.PaymentRefundInfoResponseDto;

public interface PaymentPort {

	BigDecimal refund(UUID orderId, BigDecimal refundRate);

	PaymentRefundInfoResponseDto getRefundInfo(UUID orderId);
}
