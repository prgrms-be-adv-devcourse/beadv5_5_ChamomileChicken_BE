package jabaclass.settlement.presentation.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jabaclass.settlement.infrastructure.batch.launcher.SettlementBatchJobLauncher;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/internal-batch/settlements")
@RequiredArgsConstructor
public class SettlementBatchController implements SettlementBatchApi {

	private final SettlementBatchJobLauncher settlementBatchJobLauncher;

	@PostMapping("/calculate")
	@Override
	public String calculate(
		@RequestParam(required = false) String settlementMonth
	) {
		settlementBatchJobLauncher.startCalculate(settlementMonth);
		return "정산 계산 배치 실행 요청 완료";
	}

	@PostMapping("/transfer")
	@Override
	public String transfer(
		@RequestParam(required = false) String settlementMonth
	) {
		settlementBatchJobLauncher.startTransfer(settlementMonth);
		return "정산 송금 배치 실행 요청 완료";
	}
}
