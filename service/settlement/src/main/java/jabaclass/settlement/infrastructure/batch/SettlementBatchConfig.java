package jabaclass.settlement.infrastructure.batch;

import java.time.LocalDate;
import java.time.YearMonth;

import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.infrastructure.repeat.RepeatStatus;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import jabaclass.settlement.application.usecase.SettlementCalculateUseCase;
import jabaclass.settlement.application.usecase.SettlementTargetLoadUseCase;
import jabaclass.settlement.application.usecase.SettlementTransferUseCase;

@Configuration
public class SettlementBatchConfig {

	@Bean
	public Job settlementTargetLoadJob(
		JobRepository jobRepository,
		Step settlementTargetLoadStep,
		SettlementJobExecutionListener settlementJobExecutionListener
	) {
		return new JobBuilder("settlementTargetLoadJob", jobRepository)
			.listener(settlementJobExecutionListener)
			.start(settlementTargetLoadStep)
			.build();
	}

	@Bean
	public Step settlementTargetLoadStep(
		JobRepository jobRepository,
		PlatformTransactionManager transactionManager,
		SettlementTargetLoadUseCase settlementTargetLoadUseCase
	) {
		return new StepBuilder("settlementTargetLoadStep", jobRepository)
			.tasklet((contribution, chunkContext) -> {
				String targetDateParam = (String) chunkContext.getStepContext()
					.getJobParameters()
					.get("targetDate");

				LocalDate targetDate = targetDateParam == null
					? LocalDate.now()
					: LocalDate.parse(targetDateParam);

				settlementTargetLoadUseCase.loadDailyTargets(targetDate);

				return RepeatStatus.FINISHED;
			}, transactionManager)
			.build();
	}

	@Bean
	public Job settlementCalculateJob(
		JobRepository jobRepository,
		Step settlementCalculateStep,
		SettlementJobExecutionListener settlementJobExecutionListener
	) {
		return new JobBuilder("settlementCalculateJob", jobRepository)
			.listener(settlementJobExecutionListener)
			.start(settlementCalculateStep)
			.build();
	}

	@Bean
	public Step settlementCalculateStep(
		JobRepository jobRepository,
		PlatformTransactionManager transactionManager,
		SettlementCalculateUseCase settlementCalculateUseCase
	) {
		return new StepBuilder("settlementCalculateStep", jobRepository)
			.tasklet((contribution, chunkContext) -> {
				String settlementMonthParam = (String) chunkContext.getStepContext()
					.getJobParameters()
					.get("settlementMonth");

				String settlementMonth = settlementMonthParam == null
					? YearMonth.now().minusMonths(1).toString()
					: settlementMonthParam;

				settlementCalculateUseCase.calculateMonthly(settlementMonth);

				return RepeatStatus.FINISHED;
			}, transactionManager)
			.build();
	}

	@Bean
	public Job settlementTransferJob(
		JobRepository jobRepository,
		Step settlementTransferStep,
		SettlementJobExecutionListener settlementJobExecutionListener
	) {
		return new JobBuilder("settlementTransferJob", jobRepository)
			.listener(settlementJobExecutionListener)
			.start(settlementTransferStep)
			.build();
	}

	@Bean
	public Step settlementTransferStep(
		JobRepository jobRepository,
		PlatformTransactionManager transactionManager,
		SettlementTransferUseCase settlementTransferUseCase
	) {
		return new StepBuilder("settlementTransferStep", jobRepository)
			.tasklet((contribution, chunkContext) -> {
				String settlementMonthParam = (String) chunkContext.getStepContext()
					.getJobParameters()
					.get("settlementMonth");

				String settlementMonth = settlementMonthParam == null
					? YearMonth.now().minusMonths(1).toString()
					: settlementMonthParam;

				settlementTransferUseCase.transferMonthly(settlementMonth);

				return RepeatStatus.FINISHED;
			}, transactionManager)
			.build();
	}
}
