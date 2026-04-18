package jabaclass.settlement.infrastructure.batch;

import java.time.YearMonth;
import java.util.List;

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
import org.springframework.batch.infrastructure.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import jabaclass.settlement.application.dto.SettlementTargetSummary;
import jabaclass.settlement.application.service.SettlementCalculateService;
import jabaclass.settlement.application.usecase.SettlementTransferUseCase;
import jabaclass.settlement.domain.model.SettlementTarget;
import jabaclass.settlement.domain.model.SettlementTargetCalculation;
import jabaclass.settlement.domain.repository.SettlementHistoryRepository;
import jabaclass.settlement.domain.repository.SettlementRepository;
import jabaclass.settlement.domain.repository.SettlementTargetCalculationRepository;
import jabaclass.settlement.domain.repository.SettlementTargetRepository;

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
		ItemProcessor<SettlementTarget, SettlementTargetCalculationBatchItem> settlementTargetCalculationItemProcessor,
		ItemWriter<SettlementTargetCalculationBatchItem> settlementTargetCalculationItemWriter
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
		String settlementMonth = resolveSettlementMonth(settlementMonthParam);
		return new ListItemReader<>(settlementCalculateService.findPendingTargets(settlementMonth));
	}

	@Bean
	public ItemProcessor<SettlementTarget, SettlementTargetCalculationBatchItem> settlementTargetCalculationItemProcessor(
		SettlementCalculateService settlementCalculateService
	) {
		return target -> {
			try {
				SettlementTargetCalculation calculation = settlementCalculateService.calculateTarget(target);
				settlementCalculateService.markTargetCalculated(target);
				return new SettlementTargetCalculationBatchItem(target, calculation);
			} catch (Exception e) {
				settlementCalculateService.markTargetCalculationFailed(target, e);
				return new SettlementTargetCalculationBatchItem(target, null);
			}
		};
	}

	@Bean
	public ItemWriter<SettlementTargetCalculationBatchItem> settlementTargetCalculationItemWriter(
		SettlementTargetRepository settlementTargetRepository,
		SettlementTargetCalculationRepository settlementTargetCalculationRepository
	) {
		return items -> {
			List<SettlementTarget> targets = items.getItems().stream()
				.map(SettlementTargetCalculationBatchItem::target)
				.toList();
			List<SettlementTargetCalculation> calculations = items.getItems().stream()
				.map(SettlementTargetCalculationBatchItem::calculation)
				.filter(java.util.Objects::nonNull)
				.toList();

			if (!calculations.isEmpty()) {
				settlementTargetCalculationRepository.saveAll(calculations);
			}
			if (!targets.isEmpty()) {
				settlementTargetRepository.saveAll(targets);
			}
		};
	}

	@Bean
	public Step settlementAggregationStep(
		JobRepository jobRepository,
		PlatformTransactionManager transactionManager,
		ItemReader<SettlementTargetSummary> settlementAggregationReader,
		ItemProcessor<SettlementTargetSummary, MonthlySettlementBatchItem> settlementAggregationProcessor,
		ItemWriter<MonthlySettlementBatchItem> settlementAggregationWriter
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
		String settlementMonth = resolveSettlementMonth(settlementMonthParam);
		return new ListItemReader<>(settlementTargetCalculationRepository.findSummaryBySettlementMonth(settlementMonth));
	}

	@Bean
	@StepScope
	public ItemProcessor<SettlementTargetSummary, MonthlySettlementBatchItem> settlementAggregationProcessor(
		SettlementCalculateService settlementCalculateService,
		@Value("#{jobParameters['settlementMonth']}") String settlementMonthParam
	) {
		String settlementMonth = resolveSettlementMonth(settlementMonthParam);
		return summary -> settlementCalculateService.createMonthlySettlementItem(summary, settlementMonth);
	}

	@Bean
	public ItemWriter<MonthlySettlementBatchItem> settlementAggregationWriter(
		SettlementRepository settlementRepository,
		SettlementHistoryRepository settlementHistoryRepository
	) {
		return items -> {
			List<MonthlySettlementBatchItem> validItems = items.getItems().stream()
				.filter(java.util.Objects::nonNull)
				.map(MonthlySettlementBatchItem.class::cast)
				.toList();

			if (validItems.isEmpty()) {
				return;
			}

			settlementRepository.saveAll(validItems.stream()
				.map(MonthlySettlementBatchItem::settlement)
				.toList());
			settlementHistoryRepository.saveAll(validItems.stream()
				.flatMap(item -> item.histories().stream())
				.toList());
		};
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

				String settlementMonth = resolveSettlementMonth(settlementMonthParam);

				settlementTransferUseCase.transferMonthly(settlementMonth);
				return RepeatStatus.FINISHED;
			}, transactionManager)
			.build();
	}

	private static String resolveSettlementMonth(String settlementMonthParam) {
		return settlementMonthParam == null
			? YearMonth.now().minusMonths(1).toString()
			: settlementMonthParam;
	}
}
