package jabaclass.order.presentation.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import jabaclass.order.infrastructure.client.payment.dto.PaymentRefundInfoResponseDto;

public record OrderRefundInfoResponseDto(
	UUID orderId,
	LocalDate classStartDate,
	LocalDateTime refundProcessedAt,
	BigDecimal refundRate,
	BigDecimal paymentRefundAmount,
	BigDecimal depositRefundAmount,
	BigDecimal totalRefundAmount
) {
	public static OrderRefundInfoResponseDto of(LocalDate classStartDate, PaymentRefundInfoResponseDto refundInfo) {
		return new OrderRefundInfoResponseDto(
			refundInfo.orderId(),
			classStartDate,
			refundInfo.processedAt(),
			refundInfo.refundRate(),
			refundInfo.paymentRefundAmount(),
			refundInfo.depositRefundAmount(),
			refundInfo.totalRefundAmount()
		);
	}
}
