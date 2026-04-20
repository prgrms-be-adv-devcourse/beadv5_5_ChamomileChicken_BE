package jabaclass.settlement.infrastructure.batch.component;

import java.time.YearMonth;

public final class SettlementMonthResolver {

	private SettlementMonthResolver() {
	}

	public static String resolve(String settlementMonthParam) {
		return settlementMonthParam == null
			? YearMonth.now().minusMonths(1).toString()
			: settlementMonthParam;
	}
}
