package jabaclass.payment.presentation.dto.response;

import java.util.UUID;

import jabaclass.payment.domain.model.Refund;
import jabaclass.payment.domain.model.RefundStatus;

public record RefundPaymentResponseDto(
	UUID refundId,
	UUID orderId,
	RefundStatus refundStatus
) {
	public static RefundPaymentResponseDto from(Refund refund, UUID orderId) {
		return new RefundPaymentResponseDto(
			refund.getId(),
			orderId,
			refund.getStatus()
		);
	}
}
