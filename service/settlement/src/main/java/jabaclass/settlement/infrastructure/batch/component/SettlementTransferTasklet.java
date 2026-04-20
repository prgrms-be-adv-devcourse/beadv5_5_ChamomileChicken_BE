package jabaclass.settlement.infrastructure.batch.component;

import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.StepContribution;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.infrastructure.repeat.RepeatStatus;
import org.springframework.stereotype.Component;

import jabaclass.settlement.application.usecase.SettlementTransferUseCase;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class SettlementTransferTasklet implements Tasklet {

	private final SettlementTransferUseCase settlementTransferUseCase;

	@Override
	public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {
		String settlementMonthParam = (String) chunkContext.getStepContext()
			.getJobParameters()
			.get("settlementMonth");

		String settlementMonth = SettlementMonthResolver.resolve(settlementMonthParam);
		settlementTransferUseCase.transferMonthly(settlementMonth);
		return RepeatStatus.FINISHED;
	}
}
