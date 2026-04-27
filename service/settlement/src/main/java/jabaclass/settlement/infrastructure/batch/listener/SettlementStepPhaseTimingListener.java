package jabaclass.settlement.infrastructure.batch.listener;

import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.listener.ItemProcessListener;
import org.springframework.batch.core.listener.ItemReadListener;
import org.springframework.batch.core.listener.StepExecutionListener;
import org.springframework.batch.core.listener.ItemWriteListener;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.batch.infrastructure.item.Chunk;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@StepScope
public class SettlementStepPhaseTimingListener implements
	StepExecutionListener,
	ItemReadListener<Object>,
	ItemProcessListener<Object, Object>,
	ItemWriteListener<Object> {

	private long readStartedAt;
	private long processStartedAt;
	private long writeStartedAt;

	private long totalReadNs;
	private long totalProcessNs;
	private long totalWriteNs;

	@Override
	public void beforeStep(StepExecution stepExecution) {
		totalReadNs = 0L;
		totalProcessNs = 0L;
		totalWriteNs = 0L;
	}

	@Override
	public ExitStatus afterStep(StepExecution stepExecution) {
		log.info(
			"[SETTLEMENT_BATCH_PHASE] stepName={} stepExecutionId={} totalReadMs={} totalProcessMs={} totalWriteMs={}",
			stepExecution.getStepName(),
			stepExecution.getId(),
			nanosToMillis(totalReadNs),
			nanosToMillis(totalProcessNs),
			nanosToMillis(totalWriteNs)
		);
		return null;
	}

	@Override
	public void beforeRead() {
		readStartedAt = System.nanoTime();
	}

	@Override
	public void afterRead(Object item) {
		totalReadNs += System.nanoTime() - readStartedAt;
	}

	@Override
	public void onReadError(Exception ex) {
	}

	@Override
	public void beforeProcess(Object item) {
		processStartedAt = System.nanoTime();
	}

	@Override
	public void afterProcess(Object item, Object result) {
		totalProcessNs += System.nanoTime() - processStartedAt;
	}

	@Override
	public void onProcessError(Object item, Exception e) {
	}

	@Override
	public void beforeWrite(Chunk<?> items) {
		writeStartedAt = System.nanoTime();
	}

	@Override
	public void afterWrite(Chunk<?> items) {
		totalWriteNs += System.nanoTime() - writeStartedAt;
	}

	@Override
	public void onWriteError(Exception exception, Chunk<?> items) {
	}

	private long nanosToMillis(long nanos) {
		return nanos / 1_000_000;
	}
}
