package jabaclass.settlement.infrastructure.batch;

import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.infrastructure.item.ItemProcessor;
import org.springframework.batch.infrastructure.item.ItemReader;
import org.springframework.batch.infrastructure.item.ItemWriter;
import org.springframework.batch.infrastructure.item.support.ListItemReader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import jabaclass.settlement.application.dto.SettlementTargetSummary;
import jabaclass.settlement.application.service.calculation.SettlementCalculateService;
import jabaclass.settlement.domain.model.settlement.SettlementTarget;
import jabaclass.settlement.domain.repository.SettlementTargetCalculationRepository;

@Configuration
public class SettlementBatchConfig {

	private static final int CHUNK_SIZE = 100;

	@Bean
	public Job settlementCalculateJob(
		JobRepository jobRepository,
		Step settlementTargetCalculationStep,
		Step settlementAggregationStep,
		SettlementJobExecutionListener settlementJobExecutionListener
	) {
		return new JobBuilder("settlementCalculateJob", jobRepository)
			.listener(settlementJobExecutionListener)
			.start(settlementTargetCalculationStep)
			.next(settlementAggregationStep)
			.build();
	}

	@Bean
	public Step settlementTargetCalculationStep(
		JobRepository jobRepository,
		PlatformTransactionManager transactionManager,
		ItemReader<SettlementTarget> settlementTargetItemReader,
		SettlementTargetCalculationItemProcessor settlementTargetCalculationItemProcessor,
		SettlementTargetCalculationItemWriter settlementTargetCalculationItemWriter
		) {
			return new StepBuilder("settlementTargetCalculationStep", jobRepository)
				.<SettlementTarget, SettlementTargetCalculationBatchItem>chunk(CHUNK_SIZE)
				.transactionManager(transactionManager)
				.reader(settlementTargetItemReader)
				.processor(settlementTargetCalculationItemProcessor)
				.writer(settlementTargetCalculationItemWriter)
			.build();
	}

	@Bean
	@StepScope
	public ItemReader<SettlementTarget> settlementTargetItemReader(
		SettlementCalculateService settlementCalculateService,
		@Value("#{jobParameters['settlementMonth']}") String settlementMonthParam
	) {
		String settlementMonth = SettlementMonthResolver.resolve(settlementMonthParam);
		return new ListItemReader<>(settlementCalculateService.findPendingTargets(settlementMonth));
	}

	@Bean
	public Step settlementAggregationStep(
		JobRepository jobRepository,
		PlatformTransactionManager transactionManager,
		ItemReader<SettlementTargetSummary> settlementAggregationReader,
		ItemProcessor<SettlementTargetSummary, MonthlySettlementBatchItem> settlementAggregationProcessor,
		SettlementAggregationItemWriter settlementAggregationWriter
		) {
			return new StepBuilder("settlementAggregationStep", jobRepository)
				.<SettlementTargetSummary, MonthlySettlementBatchItem>chunk(CHUNK_SIZE)
				.transactionManager(transactionManager)
				.reader(settlementAggregationReader)
				.processor(settlementAggregationProcessor)
				.writer(settlementAggregationWriter)
			.build();
	}

	@Bean
	@StepScope
	public ItemReader<SettlementTargetSummary> settlementAggregationReader(
		SettlementTargetCalculationRepository settlementTargetCalculationRepository,
		@Value("#{jobParameters['settlementMonth']}") String settlementMonthParam
	) {
		String settlementMonth = SettlementMonthResolver.resolve(settlementMonthParam);
		return new ListItemReader<>(settlementTargetCalculationRepository.findSummaryBySettlementMonth(settlementMonth));
	}

	@Bean
	@StepScope
	public ItemProcessor<SettlementTargetSummary, MonthlySettlementBatchItem> settlementAggregationProcessor(
		SettlementCalculateService settlementCalculateService,
		@Value("#{jobParameters['settlementMonth']}") String settlementMonthParam
	) {
		String settlementMonth = SettlementMonthResolver.resolve(settlementMonthParam);
		return summary -> settlementCalculateService.createMonthlySettlementItem(summary, settlementMonth);
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
		SettlementTransferTasklet settlementTransferTasklet
	) {
		return new StepBuilder("settlementTransferStep", jobRepository)
			.tasklet(settlementTransferTasklet, transactionManager)
			.build();
	}
}
