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

import jabaclass.settlement.application.service.SettlementTransferService;
import jabaclass.settlement.domain.model.settlement.Settlement;
import jabaclass.settlement.domain.model.settlement.SettlementStatus;
import jabaclass.settlement.infrastructure.batch.listener.SettlementJobExecutionListener;
import jabaclass.settlement.infrastructure.batch.listener.SettlementStepExecutionListener;
import jabaclass.settlement.infrastructure.batch.processor.SettlementTransferReconcileItemProcessor;
import jabaclass.settlement.infrastructure.batch.processor.SettlementTransferItemProcessor;
import jabaclass.settlement.infrastructure.batch.support.SettlementMonthResolver;
import jabaclass.settlement.infrastructure.batch.writer.SettlementTransferReconcileItemWriter;
import jabaclass.settlement.infrastructure.batch.writer.SettlementTransferItemWriter;

@Configuration
public class SettlementTransferJobConfig {

	private static final int CHUNK_SIZE = 100;

	private static final String SETTLEMENT_TRANSFER_QUERY = """
		select s
		from Settlement s
		where s.settlementMonth = :settlementMonth
		order by s.id
		""";

	@Bean
	public Job settlementTransferJob(
		JobRepository jobRepository,
		Step settlementTransferStep,
		Step settlementTransferReconcileStep,
		SettlementJobExecutionListener settlementJobExecutionListener
	) {
		return new JobBuilder("settlementTransferJob", jobRepository)
			.listener(settlementJobExecutionListener)
			.start(settlementTransferStep)
			.next(settlementTransferReconcileStep)
			.build();
	}

	@Bean
	public Step settlementTransferStep(
		JobRepository jobRepository,
		PlatformTransactionManager transactionManager,
		ItemReader<Settlement> settlementTransferItemReader,
		SettlementTransferItemProcessor settlementTransferItemProcessor,
		SettlementTransferItemWriter settlementTransferItemWriter,
		SettlementStepExecutionListener settlementStepExecutionListener
	) {
		return new StepBuilder("settlementTransferStep", jobRepository)
			.<Settlement, Settlement>chunk(CHUNK_SIZE)
			.transactionManager(transactionManager)
			.reader(settlementTransferItemReader)
			.processor(settlementTransferItemProcessor)
			.writer(settlementTransferItemWriter)
			.listener(settlementStepExecutionListener)
			.build();
	}

	@Bean
	public Step settlementTransferReconcileStep(
		JobRepository jobRepository,
		PlatformTransactionManager transactionManager,
		ItemReader<Settlement> settlementTransferReconcileItemReader,
		SettlementTransferReconcileItemProcessor settlementTransferReconcileItemProcessor,
		SettlementTransferReconcileItemWriter settlementTransferReconcileItemWriter,
		SettlementStepExecutionListener settlementStepExecutionListener
	) {
		return new StepBuilder("settlementTransferReconcileStep", jobRepository)
			.<Settlement, Settlement>chunk(CHUNK_SIZE)
			.transactionManager(transactionManager)
			.reader(settlementTransferReconcileItemReader)
			.processor(settlementTransferReconcileItemProcessor)
			.writer(settlementTransferReconcileItemWriter)
			.listener(settlementStepExecutionListener)
			.build();
	}

	@Bean
	@StepScope
	public JpaPagingItemReader<Settlement> settlementTransferItemReader(
		EntityManagerFactory entityManagerFactory,
		@Value("#{jobParameters['settlementMonth']}") String settlementMonthParam
	) {
		return settlementReader(
			entityManagerFactory,
			settlementMonthParam,
			"settlementTransferItemReader"
		);
	}

	@Bean
	@StepScope
	public JpaPagingItemReader<Settlement> settlementTransferReconcileItemReader(
		EntityManagerFactory entityManagerFactory,
		@Value("#{jobParameters['settlementMonth']}") String settlementMonthParam
	) {
		return settlementReader(
			entityManagerFactory,
			settlementMonthParam,
			"settlementTransferReconcileItemReader"
		);
	}

	private JpaPagingItemReader<Settlement> settlementReader(
		EntityManagerFactory entityManagerFactory,
		String settlementMonthParam,
		String name
	) {
		String settlementMonth = SettlementMonthResolver.resolve(settlementMonthParam);
		return new JpaPagingItemReaderBuilder<Settlement>()
			.name(name)
			.entityManagerFactory(entityManagerFactory)
			.pageSize(CHUNK_SIZE)
			.parameterValues(Map.of("settlementMonth", settlementMonth))
			.queryString(SETTLEMENT_TRANSFER_QUERY)
			.build();
	}

	@Bean
	@StepScope
	public SettlementTransferItemWriter settlementTransferItemWriter(
		SettlementTransferService settlementTransferService
	) {
		return new SettlementTransferItemWriter(settlementTransferService);
	}

	@Bean
	@StepScope
	public SettlementTransferReconcileItemWriter settlementTransferReconcileItemWriter(
		SettlementTransferService settlementTransferService
	) {
		return new SettlementTransferReconcileItemWriter(settlementTransferService);
	}
}
