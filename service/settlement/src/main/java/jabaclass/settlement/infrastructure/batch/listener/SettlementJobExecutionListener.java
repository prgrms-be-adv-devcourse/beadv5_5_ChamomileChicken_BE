package jabaclass.settlement.infrastructure.batch.listener;

import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.listener.JobExecutionListener;
import org.springframework.stereotype.Component;

import jabaclass.settlement.infrastructure.batch.launcher.SettlementBatchJobLauncher;
import jabaclass.settlement.infrastructure.batch.lock.SettlementBatchLockManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class SettlementJobExecutionListener implements JobExecutionListener {

	private final SettlementBatchLockManager settlementBatchLockManager;

	@Override
	public void beforeJob(JobExecution jobExecution) {
		log.info("[SETTLEMENT_BATCH] jobName={} jobExecutionId={} started",
			jobExecution.getJobInstance().getJobName(),
			jobExecution.getId());
	}

	@Override
	public void afterJob(JobExecution jobExecution) {
		try {
			if (jobExecution.getStatus() == BatchStatus.COMPLETED) {
				log.info("[SETTLEMENT_BATCH] jobName={} jobExecutionId={} completed",
					jobExecution.getJobInstance().getJobName(),
					jobExecution.getId());
				return;
			}

			log.error("[SETTLEMENT_BATCH] jobName={} jobExecutionId={} failed status={} exitDescription={}",
				jobExecution.getJobInstance().getJobName(),
				jobExecution.getId(),
				jobExecution.getStatus(),
				jobExecution.getExitStatus().getExitDescription());
		} finally {
			releaseBatchLock(jobExecution);
		}
	}

	private void releaseBatchLock(JobExecution jobExecution) {
		String lockKey = jobExecution.getJobParameters()
			.getString(SettlementBatchJobLauncher.LOCK_KEY_PARAMETER_NAME);

		settlementBatchLockManager.release(lockKey);
	}
}
