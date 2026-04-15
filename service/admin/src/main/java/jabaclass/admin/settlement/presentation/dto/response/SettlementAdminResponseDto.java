package jabaclass.admin.settlement.presentation.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import jabaclass.admin.settlement.domain.model.Settlement;
import jabaclass.admin.settlement.domain.model.SettlementStatus;

public record SettlementAdminResponseDto(
	UUID id,
	UUID sellerId,
	String settlementMonth,
	BigDecimal originalAmount,
	BigDecimal feeAmount,
	BigDecimal feeRate,
	BigDecimal settlementAmount,
	SettlementStatus status,
	LocalDateTime transferredAt,
	String failReason
) {
	public static SettlementAdminResponseDto from(Settlement settlement) {
		return new SettlementAdminResponseDto(
			settlement.getId(),
			settlement.getSellerId(),
			settlement.getSettlementMonth(),
			settlement.getOriginalAmount(),
			settlement.getFeeAmount(),
			settlement.getFeeRate(),
			settlement.getSettlementAmount(),
			settlement.getStatus(),
			settlement.getTransferredAt(),
			settlement.getFailReason()
		);
	}
}
