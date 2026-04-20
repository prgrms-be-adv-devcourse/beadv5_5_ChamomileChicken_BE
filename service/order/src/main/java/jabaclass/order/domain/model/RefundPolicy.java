package jabaclass.order.domain.model;

import java.math.BigDecimal;

public enum RefundPolicy {
	FULL(7, BigDecimal.ONE),
	PARTIAL_80(3, new BigDecimal("0.8")),
	PARTIAL_50(1, new BigDecimal("0.5")),
	PARTIAL_20(0, new BigDecimal("0.2")),
	NONE(-1, BigDecimal.ZERO);

	private final int minDays;
	private final BigDecimal rate;

	RefundPolicy(int minDays, BigDecimal rate) {
		this.minDays = minDays;
		this.rate = rate;
	}

	public static BigDecimal rateOf(long days) {
		for (RefundPolicy policy : values()) {
			if (days > policy.minDays) {
				return policy.rate;
			}
		}
		return BigDecimal.ZERO;
	}
}