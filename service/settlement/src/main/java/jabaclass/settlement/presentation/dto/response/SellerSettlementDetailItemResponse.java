package jabaclass.settlement.presentation.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import jabaclass.settlement.domain.model.settlement.SettlementTarget;
import jabaclass.settlement.domain.model.settlement.SettlementTargetCalculation;

public record SellerSettlementDetailItemResponse(
	UUID settlementTargetCalculationId,
	UUID settlementTargetId,
	UUID orderId,
	UUID paymentId,
	UUID refundId,
	UUID productId,
	String targetType,
	BigDecimal targetSettlementBaseAmount,
	BigDecimal calculatedSettlementBaseAmount,
	String calculationStatus,
	UUID appliedPromotionId,
	String appliedPromotionType,
	BigDecimal appliedFeeRate,
	UUID originalPaymentTargetCalculationId,
	LocalDateTime occurredAt,
	LocalDateTime calculatedAt
) {

	public static SellerSettlementDetailItemResponse from(
		SettlementTargetCalculation calculation,
		SettlementTarget target
	) {
		return new SellerSettlementDetailItemResponse(
			calculation.getId(),
			calculation.getSettlementTargetId(),
			target != null ? target.getOrderId() : null,
			target != null ? target.getPaymentId() : null,
			target != null ? target.getRefundId() : null,
			target != null ? target.getProductId() : null,
			target != null ? target.getTargetType().name() : null,
			target != null ? target.getSettlementBaseAmount() : null,
			calculation.getSettlementBaseAmount(),
			target != null ? target.getCalculationStatus().name() : null,
			calculation.getAppliedPromotionId(),
			calculation.getAppliedPromotionType(),
			calculation.getAppliedFeeRate(),
			calculation.getOriginalPaymentTargetCalculationId(),
			target != null ? target.getOccurredAt() : null,
			calculation.getCalculatedAt()
		);
	}
}
