package jabaclass.settlement.domain.model;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class SettlementFeePolicy {

	/**
	 * TODO:
	 * 실제 수수료율 확정되면 변경
	 * 예: 3.3%
	 */
	private static final BigDecimal DEFAULT_FEE_RATE = new BigDecimal("0.033");

	private SettlementFeePolicy() {
	}

	public static SettlementAmount calculate(BigDecimal originalAmount) {
		validateAmount(originalAmount);

		BigDecimal feeAmount = originalAmount
			.multiply(DEFAULT_FEE_RATE)
			.setScale(2, RoundingMode.DOWN);

		BigDecimal settlementAmount = originalAmount.subtract(feeAmount);

		return new SettlementAmount(
			originalAmount,
			feeAmount,
			DEFAULT_FEE_RATE,
			settlementAmount
		);
	}

	private static void validateAmount(BigDecimal originalAmount) {
		if (originalAmount == null) {
			throw new IllegalArgumentException("정산 원금은 null일 수 없습니다.");
		}
	}

	public record SettlementAmount(
		BigDecimal originalAmount,
		BigDecimal feeAmount,
		BigDecimal feeRate,
		BigDecimal settlementAmount
	) {
	}
}
