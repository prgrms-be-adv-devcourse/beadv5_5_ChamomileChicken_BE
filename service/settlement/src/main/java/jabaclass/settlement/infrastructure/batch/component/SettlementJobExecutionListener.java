package jabaclass.settlement.infrastructure.batch.component;

import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.listener.JobExecutionListener;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class SettlementJobExecutionListener implements JobExecutionListener {

	@Override
	public void beforeJob(JobExecution jobExecution) {
		log.info("[SETTLEMENT_BATCH] jobName={} jobExecutionId={} started",
			jobExecution.getJobInstance().getJobName(),
			jobExecution.getId());
	}

	@Override
	public void afterJob(JobExecution jobExecution) {
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
	}
}
