package jabaclass.settlement.infrastructure.batch.config;

import java.util.Map;

import jakarta.persistence.EntityManagerFactory;

import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.infrastructure.item.database.builder.JpaPagingItemReaderBuilder;
import org.springframework.batch.infrastructure.item.ItemProcessor;
import org.springframework.batch.infrastructure.item.ItemReader;
import org.springframework.batch.infrastructure.item.ItemWriter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import jabaclass.settlement.application.dto.SettlementTargetSummary;
import jabaclass.settlement.application.service.calculation.SettlementCalculateService;
import jabaclass.settlement.domain.model.settlement.SettlementTarget;
import jabaclass.settlement.domain.model.settlement.SettlementTargetCalculationStatus;
import jabaclass.settlement.infrastructure.batch.component.SettlementAggregationItemWriter;
import jabaclass.settlement.infrastructure.batch.component.SettlementJobExecutionListener;
import jabaclass.settlement.infrastructure.batch.component.SettlementMonthResolver;
import jabaclass.settlement.infrastructure.batch.component.SettlementTargetCalculationItemProcessor;
import jabaclass.settlement.infrastructure.batch.component.SettlementTargetCalculationItemWriter;
import jabaclass.settlement.infrastructure.batch.component.SettlementTransferTasklet;
import jabaclass.settlement.infrastructure.batch.dto.MonthlySettlementBatchItem;
import jabaclass.settlement.infrastructure.batch.dto.SettlementTargetCalculationBatchItem;

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
		EntityManagerFactory entityManagerFactory,
		@Value("#{jobParameters['settlementMonth']}") String settlementMonthParam
	) {
		String settlementMonth = SettlementMonthResolver.resolve(settlementMonthParam);
		return new JpaPagingItemReaderBuilder<SettlementTarget>()
			.name("settlementTargetItemReader")
			.entityManagerFactory(entityManagerFactory)
			.pageSize(CHUNK_SIZE)
			.parameterValues(Map.of(
				"settlementMonth", settlementMonth,
				"calculationStatus", SettlementTargetCalculationStatus.PENDING
			))
			.queryString("""
				select st
				from SettlementTarget st
				where st.settlementMonth = :settlementMonth
				  and st.calculationStatus = :calculationStatus
				order by st.id
				""")
			.build();
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
		EntityManagerFactory entityManagerFactory,
		@Value("#{jobParameters['settlementMonth']}") String settlementMonthParam
	) {
		String settlementMonth = SettlementMonthResolver.resolve(settlementMonthParam);
		return new JpaPagingItemReaderBuilder<SettlementTargetSummary>()
			.name("settlementAggregationReader")
			.entityManagerFactory(entityManagerFactory)
			.pageSize(CHUNK_SIZE)
			.parameterValues(Map.of("settlementMonth", settlementMonth))
			.queryString("""
				select new jabaclass.settlement.application.dto.SettlementTargetSummary(
					stc.sellerId,
					stc.settlementMonth,
					sum(stc.settlementBaseAmount)
				)
				from SettlementTargetCalculation stc
				where stc.settlementMonth = :settlementMonth
				group by stc.sellerId, stc.settlementMonth
				order by stc.sellerId
				""")
			.build();
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
