package jabaclass.settlement.domain.model.settlement;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import jabaclass.settlement.domain.model.BaseEntity;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
	name = "settlement_target_calculations",
	indexes = {
		@Index(
			name = "idx_stc_month_seller",
			columnList = "settlement_month, seller_id"
		)
	}
)
public class SettlementTargetCalculation extends BaseEntity {

	@Column(name = "settlement_target_id", nullable = false, unique = true)
	private UUID settlementTargetId;

	@Column(name = "settlement_month", nullable = false, length = 7)
	private String settlementMonth;

	@Column(name = "seller_id", nullable = false)
	private UUID sellerId;

	@Column(name = "settlement_base_amount", nullable = false, precision = 19, scale = 2)
	private BigDecimal settlementBaseAmount;

	@Column(name = "applied_promotion_id")
	private UUID appliedPromotionId;

	@Column(name = "applied_promotion_type", length = 50)
	private String appliedPromotionType;

	@Column(name = "applied_fee_rate", precision = 10, scale = 4)
	private BigDecimal appliedFeeRate;

	@Column(name = "original_payment_target_calculation_id")
	private UUID originalPaymentTargetCalculationId;

	@Column(name = "calculated_at", nullable = false)
	private LocalDateTime calculatedAt;

	private SettlementTargetCalculation(
		UUID settlementTargetId,
		String settlementMonth,
		UUID sellerId,
		BigDecimal settlementBaseAmount,
		UUID appliedPromotionId,
		String appliedPromotionType,
		BigDecimal appliedFeeRate,
		UUID originalPaymentTargetCalculationId
	) {
		this.settlementTargetId = settlementTargetId;
		this.settlementMonth = settlementMonth;
		this.sellerId = sellerId;
		this.settlementBaseAmount = settlementBaseAmount;
		this.appliedPromotionId = appliedPromotionId;
		this.appliedPromotionType = appliedPromotionType;
		this.appliedFeeRate = appliedFeeRate;
		this.originalPaymentTargetCalculationId = originalPaymentTargetCalculationId;
		this.calculatedAt = LocalDateTime.now();
	}

	public static SettlementTargetCalculation forPayment(
		SettlementTarget target,
		UUID appliedPromotionId,
		String appliedPromotionType,
		BigDecimal appliedFeeRate
	) {
		BigDecimal settlementBaseAmount = target.getSettlementBaseAmount();

		return new SettlementTargetCalculation(
			target.getId(),
			target.getSettlementMonth(),
			target.getSellerId(),
			settlementBaseAmount,
			appliedPromotionId,
			appliedPromotionType,
			appliedFeeRate,
			null
		);
	}

	public static SettlementTargetCalculation forRefund(
		SettlementTarget target,
		SettlementTarget originalPaymentTarget,
		SettlementTargetCalculation originalPaymentCalculation
	) {
		BigDecimal refundAmount = target.getSettlementBaseAmount().abs();
		BigDecimal ratio = refundAmount
			.divide(originalPaymentTarget.getSettlementBaseAmount(), 8, RoundingMode.HALF_UP);
		BigDecimal refundSettlementBaseAmount = originalPaymentCalculation.getSettlementBaseAmount()
			.multiply(ratio)
			.setScale(2, RoundingMode.DOWN)
			.negate();

		return new SettlementTargetCalculation(
			target.getId(),
			target.getSettlementMonth(),
			target.getSellerId(),
			refundSettlementBaseAmount,
			originalPaymentCalculation.getAppliedPromotionId(),
			originalPaymentCalculation.getAppliedPromotionType(),
			originalPaymentCalculation.getAppliedFeeRate(),
			originalPaymentCalculation.getId()
		);
	}

	public static SettlementTargetCalculation forRefundWithPromotion(
		SettlementTarget target,
		UUID appliedPromotionId,
		String appliedPromotionType,
		BigDecimal appliedFeeRate
	) {
		return new SettlementTargetCalculation(
			target.getId(),
			target.getSettlementMonth(),
			target.getSellerId(),
			target.getSettlementBaseAmount(),
			appliedPromotionId,
			appliedPromotionType,
			appliedFeeRate,
			null
		);
	}
}
