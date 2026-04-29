package jabaclass.settlement.infrastructure.batch.listener;

import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.listener.ChunkListener;
import org.springframework.batch.core.listener.StepExecutionListener;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@StepScope
public class SettlementChunkProgressListener implements StepExecutionListener, ChunkListener {

	private static final String STEP_STARTED_AT_KEY = "chunkProgressStepStartedAt";
	private static final String CHUNK_STARTED_AT_KEY = "chunkProgressChunkStartedAt";
	private static final String PREVIOUS_READ_COUNT_KEY = "chunkProgressPreviousReadCount";
	private static final String PREVIOUS_WRITE_COUNT_KEY = "chunkProgressPreviousWriteCount";
	private static final String PREVIOUS_FILTER_COUNT_KEY = "chunkProgressPreviousFilterCount";
	private static final String PREVIOUS_SKIP_COUNT_KEY = "chunkProgressPreviousSkipCount";
	private static final String CHUNK_INDEX_KEY = "chunkProgressChunkIndex";

	@Override
	public void beforeStep(StepExecution stepExecution) {
		stepExecution.getExecutionContext().putLong(STEP_STARTED_AT_KEY, System.currentTimeMillis());
		stepExecution.getExecutionContext().putLong(PREVIOUS_READ_COUNT_KEY, 0L);
		stepExecution.getExecutionContext().putLong(PREVIOUS_WRITE_COUNT_KEY, 0L);
		stepExecution.getExecutionContext().putLong(PREVIOUS_FILTER_COUNT_KEY, 0L);
		stepExecution.getExecutionContext().putLong(PREVIOUS_SKIP_COUNT_KEY, 0L);
		stepExecution.getExecutionContext().putLong(CHUNK_INDEX_KEY, 0L);
	}

	@Override
	public void beforeChunk(ChunkContext context) {
		context.getStepContext()
			.getStepExecution()
			.getExecutionContext()
			.putLong(CHUNK_STARTED_AT_KEY, System.currentTimeMillis());
	}

	@Override
	public void afterChunk(ChunkContext context) {
		StepExecution stepExecution = context.getStepContext().getStepExecution();
		long chunkStartedAt = stepExecution.getExecutionContext().getLong(CHUNK_STARTED_AT_KEY, System.currentTimeMillis());
		long stepStartedAt = stepExecution.getExecutionContext().getLong(STEP_STARTED_AT_KEY, System.currentTimeMillis());
		long previousReadCount = stepExecution.getExecutionContext().getLong(PREVIOUS_READ_COUNT_KEY, 0L);
		long previousWriteCount = stepExecution.getExecutionContext().getLong(PREVIOUS_WRITE_COUNT_KEY, 0L);
		long previousFilterCount = stepExecution.getExecutionContext().getLong(PREVIOUS_FILTER_COUNT_KEY, 0L);
		long previousSkipCount = stepExecution.getExecutionContext().getLong(PREVIOUS_SKIP_COUNT_KEY, 0L);
		long chunkIndex = stepExecution.getExecutionContext().getLong(CHUNK_INDEX_KEY, 0L) + 1L;

		long currentReadCount = stepExecution.getReadCount();
		long currentWriteCount = stepExecution.getWriteCount();
		long currentFilterCount = stepExecution.getFilterCount();
		long currentSkipCount = stepExecution.getSkipCount();
		long chunkElapsedMs = System.currentTimeMillis() - chunkStartedAt;
		long totalElapsedMs = System.currentTimeMillis() - stepStartedAt;
		long chunkReadCount = currentReadCount - previousReadCount;
		long chunkWriteCount = currentWriteCount - previousWriteCount;
		long chunkFilterCount = currentFilterCount - previousFilterCount;
		long chunkSkipCount = currentSkipCount - previousSkipCount;
		long throughputPerSec = totalElapsedMs <= 0 ? 0L : (currentReadCount * 1000L) / totalElapsedMs;

		log.info(
			"[SETTLEMENT_BATCH_CHUNK] stepName={} stepExecutionId={} chunkIndex={} chunkReadCount={} chunkWriteCount={} chunkFilterCount={} chunkSkipCount={} totalReadCount={} totalWriteCount={} totalFilterCount={} totalSkipCount={} chunkElapsedMs={} totalElapsedMs={} throughputPerSec={}",
			stepExecution.getStepName(),
			stepExecution.getId(),
			chunkIndex,
			chunkReadCount,
			chunkWriteCount,
			chunkFilterCount,
			chunkSkipCount,
			currentReadCount,
			currentWriteCount,
			currentFilterCount,
			currentSkipCount,
			chunkElapsedMs,
			totalElapsedMs,
			throughputPerSec
		);

		stepExecution.getExecutionContext().putLong(PREVIOUS_READ_COUNT_KEY, currentReadCount);
		stepExecution.getExecutionContext().putLong(PREVIOUS_WRITE_COUNT_KEY, currentWriteCount);
		stepExecution.getExecutionContext().putLong(PREVIOUS_FILTER_COUNT_KEY, currentFilterCount);
		stepExecution.getExecutionContext().putLong(PREVIOUS_SKIP_COUNT_KEY, currentSkipCount);
		stepExecution.getExecutionContext().putLong(CHUNK_INDEX_KEY, chunkIndex);
	}

	@Override
	public void afterChunkError(ChunkContext context) {
		StepExecution stepExecution = context.getStepContext().getStepExecution();
		log.warn(
			"[SETTLEMENT_BATCH_CHUNK] stepName={} stepExecutionId={} chunkError readCount={} writeCount={} filterCount={} skipCount={}",
			stepExecution.getStepName(),
			stepExecution.getId(),
			stepExecution.getReadCount(),
			stepExecution.getWriteCount(),
			stepExecution.getFilterCount(),
			stepExecution.getSkipCount()
		);
	}
}
