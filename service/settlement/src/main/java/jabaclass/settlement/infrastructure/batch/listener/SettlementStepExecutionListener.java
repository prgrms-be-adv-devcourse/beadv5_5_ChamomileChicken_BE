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
		stepExecution.getExecutionContext().putLong("stepStartTime", System.currentTimeMillis());
		log.info("[SETTLEMENT_BATCH] stepName={} stepExecutionId={} started",
			stepExecution.getStepName(),
			stepExecution.getId());
	}

	@Override
	public ExitStatus afterStep(StepExecution stepExecution) {
		long startedAt = stepExecution.getExecutionContext().getLong("stepStartTime", System.currentTimeMillis());
		long elapsedMs = System.currentTimeMillis() - startedAt;
		log.info(
			"[SETTLEMENT_BATCH] stepName={} stepExecutionId={} status={} readCount={} writeCount={} filterCount={} skipCount={} elapsedMs={}",
			stepExecution.getStepName(),
			stepExecution.getId(),
			stepExecution.getStatus(),
			stepExecution.getReadCount(),
			stepExecution.getWriteCount(),
			stepExecution.getFilterCount(),
			stepExecution.getSkipCount(),
			elapsedMs
		);
		return null;
	}
}
