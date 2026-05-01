package jabaclass.settlement.application.service.calculation;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import org.springframework.stereotype.Component;

import jabaclass.settlement.application.dto.SettlementFeeRateAmount;

@Component
public class SettlementFeeCalculator {

	public BigDecimal calculateFeeAmount(BigDecimal settlementBaseAmount, BigDecimal feeRate) {
		return settlementBaseAmount.multiply(feeRate).setScale(2, RoundingMode.DOWN);
	}

	public BigDecimal calculateFeeAmount(
		BigDecimal summaryBaseAmount,
		BigDecimal defaultFeeRate,
		List<SettlementFeeRateAmount> feeRateAmounts
	) {
		if (feeRateAmounts == null || feeRateAmounts.isEmpty()) {
			return calculateFeeAmount(summaryBaseAmount, defaultFeeRate);
		}

		return feeRateAmounts.stream()
			.map(feeRateAmount -> calculateFeeAmount(
				feeRateAmount.settlementBaseAmount(),
				resolveAppliedFeeRate(feeRateAmount.appliedFeeRate(), defaultFeeRate)
			))
			.reduce(BigDecimal.ZERO, BigDecimal::add);
	}

	public BigDecimal calculateSettlementAmount(
		BigDecimal summaryBaseAmount,
		BigDecimal defaultFeeRate,
		List<SettlementFeeRateAmount> feeRateAmounts
	) {
		return summaryBaseAmount.subtract(calculateFeeAmount(summaryBaseAmount, defaultFeeRate, feeRateAmounts));
	}

	public BigDecimal resolveAppliedFeeRate(BigDecimal appliedFeeRate, BigDecimal defaultFeeRate) {
		return appliedFeeRate == null ? defaultFeeRate : appliedFeeRate;
	}
}
