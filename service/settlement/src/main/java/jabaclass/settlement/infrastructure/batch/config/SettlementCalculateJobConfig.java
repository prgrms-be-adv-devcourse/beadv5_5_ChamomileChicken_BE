package jabaclass.settlement.infrastructure.batch.config;

import java.util.Map;

import jakarta.persistence.EntityManagerFactory;

import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.infrastructure.item.ItemReader;
import org.springframework.batch.infrastructure.item.database.JpaPagingItemReader;
import org.springframework.batch.infrastructure.item.database.builder.JpaPagingItemReaderBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import jabaclass.settlement.application.dto.SettlementTargetSummary;
import jabaclass.settlement.application.service.calculation.SettlementCalculateService;
import jabaclass.settlement.domain.model.settlement.SettlementTarget;
import jabaclass.settlement.infrastructure.batch.dto.SettlementTargetCalculationBatchItem;
import jabaclass.settlement.infrastructure.batch.listener.SettlementJobExecutionListener;
import jabaclass.settlement.infrastructure.batch.listener.SettlementStepExecutionListener;
import jabaclass.settlement.infrastructure.batch.processor.SettlementTargetCalculationItemProcessor;
import jabaclass.settlement.infrastructure.batch.support.SettlementMonthResolver;
import jabaclass.settlement.infrastructure.batch.writer.SettlementAggregationItemWriter;
import jabaclass.settlement.infrastructure.batch.writer.SettlementTargetCalculationItemWriter;

@Configuration
public class SettlementCalculateJobConfig {

	private static final int CHUNK_SIZE = 100;

	private static final String SETTLEMENT_TARGET_QUERY = """
		select st
		from SettlementTarget st
		where st.settlementMonth = :settlementMonth
		order by st.occurredAt, st.id
		""";

	private static final String SETTLEMENT_AGGREGATION_QUERY = """
		select new jabaclass.settlement.application.dto.SettlementTargetSummary(
			stc.sellerId,
			stc.settlementMonth,
			sum(stc.settlementBaseAmount)
		)
		from SettlementTargetCalculation stc
		where stc.settlementMonth = :settlementMonth
		group by stc.sellerId, stc.settlementMonth
		order by stc.sellerId
		""";

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
		SettlementTargetCalculationItemWriter settlementTargetCalculationItemWriter,
		SettlementStepExecutionListener settlementStepExecutionListener
	) {
		return new StepBuilder("settlementTargetCalculationStep", jobRepository)
			.<SettlementTarget, SettlementTargetCalculationBatchItem>chunk(CHUNK_SIZE)
			.transactionManager(transactionManager)
			.reader(settlementTargetItemReader)
			.processor(settlementTargetCalculationItemProcessor)
			.writer(settlementTargetCalculationItemWriter)
			.listener(settlementStepExecutionListener)
			.build();
	}

	@Bean
	@StepScope
	public JpaPagingItemReader<SettlementTarget> settlementTargetItemReader(
		EntityManagerFactory entityManagerFactory,
		@Value("#{jobParameters['settlementMonth']}") String settlementMonthParam
	) {
		String settlementMonth = SettlementMonthResolver.resolve(settlementMonthParam);
		return new JpaPagingItemReaderBuilder<SettlementTarget>()
			.name("settlementTargetItemReader")
			.entityManagerFactory(entityManagerFactory)
			.pageSize(CHUNK_SIZE)
			.parameterValues(Map.of("settlementMonth", settlementMonth))
			.queryString(SETTLEMENT_TARGET_QUERY)
			.build();
	}

	@Bean
	public Step settlementAggregationStep(
		JobRepository jobRepository,
		PlatformTransactionManager transactionManager,
		ItemReader<SettlementTargetSummary> settlementAggregationReader,
		SettlementAggregationItemWriter settlementAggregationWriter,
		SettlementStepExecutionListener settlementStepExecutionListener
	) {
		return new StepBuilder("settlementAggregationStep", jobRepository)
			.<SettlementTargetSummary, SettlementTargetSummary>chunk(CHUNK_SIZE)
			.transactionManager(transactionManager)
			.reader(settlementAggregationReader)
			.writer(settlementAggregationWriter)
			.listener(settlementStepExecutionListener)
			.build();
	}

	@Bean
	@StepScope
	public JpaPagingItemReader<SettlementTargetSummary> settlementAggregationReader(
		EntityManagerFactory entityManagerFactory,
		@Value("#{jobParameters['settlementMonth']}") String settlementMonthParam
	) {
		String settlementMonth = SettlementMonthResolver.resolve(settlementMonthParam);
		return new JpaPagingItemReaderBuilder<SettlementTargetSummary>()
			.name("settlementAggregationReader")
			.entityManagerFactory(entityManagerFactory)
			.pageSize(CHUNK_SIZE)
			.parameterValues(Map.of("settlementMonth", settlementMonth))
			.queryString(SETTLEMENT_AGGREGATION_QUERY)
			.build();
	}

	@Bean
	@StepScope
	public SettlementAggregationItemWriter settlementAggregationWriter(
		SettlementCalculateService settlementCalculateService,
		@Value("#{jobParameters['settlementMonth']}") String settlementMonthParam
	) {
		String settlementMonth = SettlementMonthResolver.resolve(settlementMonthParam);
		return new SettlementAggregationItemWriter(
			settlementCalculateService,
			settlementMonth,
			settlementCalculateService.findActiveSellerGradePolicies()
		);
	}
}
