package jabaclass.settlement.domain.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "settlement_target_calculations")
public class SettlementTargetCalculation extends BaseEntity {

	@Column(name = "settlement_target_id", nullable = false, unique = true)
	private UUID settlementTargetId;

	@Column(name = "settlement_month", nullable = false, length = 7)
	private String settlementMonth;

	@Column(name = "seller_id", nullable = false)
	private UUID sellerId;

	@Column(name = "settlement_base_amount", nullable = false, precision = 19, scale = 2)
	private BigDecimal settlementBaseAmount;

	@Enumerated(EnumType.STRING)
	@Column(name = "seller_grade_code", nullable = false, length = 30)
	private SellerGradeType sellerGradeCode;

	@Column(name = "seller_grade_policy_id", nullable = false)
	private UUID sellerGradePolicyId;

	@Column(name = "applied_promotion_id")
	private UUID appliedPromotionId;

	@Column(name = "applied_promotion_type", length = 50)
	private String appliedPromotionType;

	@Column(name = "fee_rate", nullable = false, precision = 10, scale = 4)
	private BigDecimal feeRate;

	@Column(name = "fee_amount", nullable = false, precision = 19, scale = 2)
	private BigDecimal feeAmount;

	@Column(name = "settlement_amount", nullable = false, precision = 19, scale = 2)
	private BigDecimal settlementAmount;

	@Column(name = "original_payment_target_calculation_id")
	private UUID originalPaymentTargetCalculationId;

	@Column(name = "calculated_at", nullable = false)
	private LocalDateTime calculatedAt;

	private SettlementTargetCalculation(
		UUID settlementTargetId,
		String settlementMonth,
		UUID sellerId,
		BigDecimal settlementBaseAmount,
		SellerGradeType sellerGradeCode,
		UUID sellerGradePolicyId,
		UUID appliedPromotionId,
		String appliedPromotionType,
		BigDecimal feeRate,
		BigDecimal feeAmount,
		BigDecimal settlementAmount,
		UUID originalPaymentTargetCalculationId
	) {
		this.settlementTargetId = settlementTargetId;
		this.settlementMonth = settlementMonth;
		this.sellerId = sellerId;
		this.settlementBaseAmount = settlementBaseAmount;
		this.sellerGradeCode = sellerGradeCode;
		this.sellerGradePolicyId = sellerGradePolicyId;
		this.appliedPromotionId = appliedPromotionId;
		this.appliedPromotionType = appliedPromotionType;
		this.feeRate = feeRate;
		this.feeAmount = feeAmount;
		this.settlementAmount = settlementAmount;
		this.originalPaymentTargetCalculationId = originalPaymentTargetCalculationId;
		this.calculatedAt = LocalDateTime.now();
	}

	public static SettlementTargetCalculation forPayment(
		SettlementTarget target,
		SellerGradePolicy sellerGradePolicy,
		UUID appliedPromotionId,
		String appliedPromotionType,
		BigDecimal adjustedFeeRate
	) {
		BigDecimal settlementBaseAmount = target.getGrossAmount();
		BigDecimal feeAmount = calculateFeeAmount(settlementBaseAmount, adjustedFeeRate);
		BigDecimal settlementAmount = settlementBaseAmount.subtract(feeAmount);

		return new SettlementTargetCalculation(
			target.getId(),
			target.getSettlementMonth(),
			target.getSellerId(),
			settlementBaseAmount,
			sellerGradePolicy.getGradeCode(),
			sellerGradePolicy.getId(),
			appliedPromotionId,
			appliedPromotionType,
			adjustedFeeRate,
			feeAmount,
			settlementAmount,
			null
		);
	}

	public static SettlementTargetCalculation forRefund(
		SettlementTarget target,
		SettlementTarget originalPaymentTarget,
		SettlementTargetCalculation originalPaymentCalculation
	) {
		BigDecimal refundAmount = target.getGrossAmount().abs();
		BigDecimal ratio = refundAmount
			.divide(originalPaymentTarget.getGrossAmount(), 8, RoundingMode.HALF_UP);
		BigDecimal refundSettlementBaseAmount = originalPaymentCalculation.getSettlementBaseAmount()
			.multiply(ratio)
			.setScale(2, RoundingMode.DOWN)
			.negate();
		BigDecimal refundFeeAmount = originalPaymentCalculation.getFeeAmount()
			.multiply(ratio)
			.setScale(2, RoundingMode.DOWN)
			.negate();
		BigDecimal refundSettlementAmount = refundSettlementBaseAmount.subtract(refundFeeAmount);

		return new SettlementTargetCalculation(
			target.getId(),
			target.getSettlementMonth(),
			target.getSellerId(),
			refundSettlementBaseAmount,
			originalPaymentCalculation.getSellerGradeCode(),
			originalPaymentCalculation.getSellerGradePolicyId(),
			originalPaymentCalculation.getAppliedPromotionId(),
			originalPaymentCalculation.getAppliedPromotionType(),
			originalPaymentCalculation.getFeeRate(),
			refundFeeAmount,
			refundSettlementAmount,
			originalPaymentCalculation.getId()
		);
	}

	private static BigDecimal calculateFeeAmount(BigDecimal settlementBaseAmount, BigDecimal feeRate) {
		return settlementBaseAmount.multiply(feeRate).setScale(2, RoundingMode.DOWN);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof SettlementTargetCalculation that)) return false;
		return Objects.equals(getId(), that.getId());
	}

	@Override
	public int hashCode() {
		return Objects.hash(getId());
	}
}
