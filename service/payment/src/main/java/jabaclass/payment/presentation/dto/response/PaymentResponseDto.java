package jabaclass.payment.presentation.dto.response;

import jabaclass.payment.domain.model.Payment;

import java.math.BigDecimal;
import java.util.UUID;

public record PaymentResponseDto(
        UUID paymentId,
        UUID orderId,
        BigDecimal totalAmount
) {
    public static PaymentResponseDto from(Payment payment) {
        return new PaymentResponseDto(
                payment.getId(),
                payment.getOrderId(),
                payment.getTotalAmount()
        );
    }
}
