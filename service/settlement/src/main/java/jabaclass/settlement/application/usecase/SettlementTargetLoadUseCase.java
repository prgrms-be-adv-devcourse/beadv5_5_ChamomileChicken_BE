package jabaclass.settlement.application.usecase;

import java.time.LocalDate;

public interface SettlementTargetLoadUseCase {
	void loadDailyTargets(LocalDate targetDate);
}