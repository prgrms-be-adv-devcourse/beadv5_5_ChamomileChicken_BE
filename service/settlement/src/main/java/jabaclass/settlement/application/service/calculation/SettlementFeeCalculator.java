package jabaclass.settlement.application.service.calculation;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import org.springframework.stereotype.Component;

import jabaclass.settlement.domain.model.settlement.SettlementTargetCalculation;

@Component
public class SettlementFeeCalculator {

	public BigDecimal calculateFeeAmount(BigDecimal settlementBaseAmount, BigDecimal feeRate) {
		return settlementBaseAmount.multiply(feeRate).setScale(2, RoundingMode.DOWN);
	}

	public BigDecimal calculateFeeAmount(
		BigDecimal summaryBaseAmount,
		BigDecimal defaultFeeRate,
		List<SettlementTargetCalculation> calculations
	) {
		if (calculations == null || calculations.isEmpty()) {
			return calculateFeeAmount(summaryBaseAmount, defaultFeeRate);
		}

		return calculations.stream()
			.map(calculation -> calculateFeeAmount(
				calculation.getSettlementBaseAmount(),
				resolveAppliedFeeRate(calculation, defaultFeeRate)
			))
			.reduce(BigDecimal.ZERO, BigDecimal::add);
	}

	public BigDecimal calculateSettlementAmount(
		BigDecimal summaryBaseAmount,
		BigDecimal defaultFeeRate,
		List<SettlementTargetCalculation> calculations
	) {
		return summaryBaseAmount.subtract(calculateFeeAmount(summaryBaseAmount, defaultFeeRate, calculations));
	}

	public BigDecimal resolveAppliedFeeRate(SettlementTargetCalculation calculation, BigDecimal defaultFeeRate) {
		return calculation.getAppliedFeeRate() == null ? defaultFeeRate : calculation.getAppliedFeeRate();
	}
}
