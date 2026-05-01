package jabaclass.settlement.infrastructure.batch.dto;

import java.time.LocalDateTime;

import jabaclass.settlement.application.dto.SettlementTargetInfo;
import jabaclass.settlement.domain.model.settlement.SettlementTargetCalculation;
import jabaclass.settlement.domain.model.settlement.SettlementTargetCalculationStatus;

public record SettlementTargetCalculationBatchItem(
	SettlementTargetInfo target,
	SettlementTargetCalculation calculation,
	SettlementTargetCalculationStatus calculationStatus,
	LocalDateTime calculationCompletedAt,
	String calculationFailedReason
) {

	public static SettlementTargetCalculationBatchItem calculated(
		SettlementTargetInfo target,
		SettlementTargetCalculation calculation
	) {
		return new SettlementTargetCalculationBatchItem(
			target,
			calculation,
			SettlementTargetCalculationStatus.CALCULATED,
			LocalDateTime.now(),
			null
		);
	}

	public static SettlementTargetCalculationBatchItem failed(
		SettlementTargetInfo target,
		String failedReason
	) {
		return new SettlementTargetCalculationBatchItem(
			target,
			null,
			SettlementTargetCalculationStatus.FAILED,
			LocalDateTime.now(),
			failedReason
		);
	}
}
