package jabaclass.order.application.port.external;

import java.math.BigDecimal;
import java.util.UUID;

import jabaclass.order.infrastructure.client.payment.dto.InternalRefundResponseDto;

public interface PaymentPort {

    InternalRefundResponseDto refund(UUID orderId, BigDecimal refundRate);
}
