package jabaclass.payment.presentation.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import jabaclass.payment.domain.model.Refund;

public record InternalRefundDetailResponseDto(
	UUID orderId,
	BigDecimal refundRate,
	BigDecimal paymentRefundAmount,
	BigDecimal depositRefundAmount,
	BigDecimal totalRefundAmount,
	LocalDateTime requestedAt,
	LocalDateTime processedAt
) {
	public static InternalRefundDetailResponseDto from(UUID orderId, Refund refund) {
		return new InternalRefundDetailResponseDto(
			orderId,
			refund.getRefundRate(),
			refund.getPaymentRefundAmount(),
			refund.getDepositRefundAmount(),
			refund.getTotalRefundAmount(),
			refund.getRequestedAt(),
			refund.getProcessedAt()
		);
	}
}
