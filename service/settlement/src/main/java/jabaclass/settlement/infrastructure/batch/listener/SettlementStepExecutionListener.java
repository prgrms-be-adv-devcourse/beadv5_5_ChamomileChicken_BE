package jabaclass.settlement.infrastructure.batch.listener;

import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.listener.StepExecutionListener;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class SettlementStepExecutionListener implements StepExecutionListener {

	@Override
	public void beforeStep(StepExecution stepExecution) {
		log.info("[SETTLEMENT_BATCH] stepName={} stepExecutionId={} started",
			stepExecution.getStepName(),
			stepExecution.getId());
	}

	@Override
	public ExitStatus afterStep(StepExecution stepExecution) {
		log.info(
			"[SETTLEMENT_BATCH] stepName={} stepExecutionId={} status={} readCount={} writeCount={} filterCount={} skipCount={}",
			stepExecution.getStepName(),
			stepExecution.getId(),
			stepExecution.getStatus(),
			stepExecution.getReadCount(),
			stepExecution.getWriteCount(),
			stepExecution.getFilterCount(),
			stepExecution.getSkipCount()
		);
		return null;
	}
}
