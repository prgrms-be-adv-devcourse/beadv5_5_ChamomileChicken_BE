package jabaclass.settlement.presentation.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import jabaclass.settlement.domain.model.Settlement;

public record SettlementResponse(
	UUID id,
	UUID sellerId,
	String settlementMonth,
	BigDecimal originalAmount,
	String sellerGradeCode,
	UUID sellerGradePolicyId,
	BigDecimal gradeBaseAmount,
	BigDecimal feeAmount,
	BigDecimal feeRate,
	BigDecimal settlementAmount,
	String status,
	LocalDateTime transferredAt,
	String failReason
) {

	public static SettlementResponse from(Settlement settlement) {
		return new SettlementResponse(
			settlement.getId(),
			settlement.getSellerId(),
			settlement.getSettlementMonth(),
			settlement.getOriginalAmount(),
			settlement.getSellerGradeCode().name(),
			settlement.getSellerGradePolicyId(),
			settlement.getGradeBaseAmount(),
			settlement.getFeeAmount(),
			settlement.getFeeRate(),
			settlement.getSettlementAmount(),
			settlement.getStatus().name(),
			settlement.getTransferredAt(),
			settlement.getFailReason()
		);
	}
}
