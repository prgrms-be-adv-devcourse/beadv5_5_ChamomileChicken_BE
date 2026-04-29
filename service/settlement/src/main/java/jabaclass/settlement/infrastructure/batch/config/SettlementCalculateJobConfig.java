package jabaclass.settlement.infrastructure.batch.config;

import java.util.List;
import java.util.Map;

import jakarta.persistence.EntityManagerFactory;

import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.infrastructure.item.ItemProcessor;
import org.springframework.batch.infrastructure.item.ItemStreamReader;
import org.springframework.batch.infrastructure.item.database.JpaPagingItemReader;
import org.springframework.batch.infrastructure.item.database.builder.JpaPagingItemReaderBuilder;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import jabaclass.settlement.application.dto.MonthlySettlementCreationItem;
import jabaclass.settlement.application.dto.SellerGradeCalculationItem;
import jabaclass.settlement.application.dto.SettlementTargetSummary;
import jabaclass.settlement.application.exception.BusinessException;
import jabaclass.settlement.application.exception.SettlementCalculationRetryableException;
import jabaclass.settlement.application.service.calculation.SettlementCalculateService;
import jabaclass.settlement.domain.model.grade.SellerGrade;
import jabaclass.settlement.domain.model.grade.SellerGradePolicy;
import jabaclass.settlement.domain.repository.SellerPromotionRepository;
import jabaclass.settlement.domain.repository.SellerGradeRepository;
import jabaclass.settlement.domain.repository.SettlementPromotionRepository;
import jabaclass.settlement.domain.repository.SettlementRepository;
import jabaclass.settlement.domain.repository.SettlementTargetCalculationRepository;
import jabaclass.settlement.domain.repository.SettlementTargetRepository;
import jabaclass.settlement.domain.model.settlement.Settlement;
import jabaclass.settlement.domain.model.settlement.SettlementTarget;
import jabaclass.settlement.domain.model.settlement.SettlementTargetCalculationStatus;
import jabaclass.settlement.domain.model.settlement.SettlementTargetType;
import jabaclass.settlement.infrastructure.batch.dto.PaymentTargetCalculationItem;
import jabaclass.settlement.infrastructure.batch.dto.RefundTargetCalculationItem;
import jabaclass.settlement.infrastructure.batch.dto.SettlementTargetCalculationBatchItem;
import jabaclass.settlement.infrastructure.batch.listener.SettlementJobExecutionListener;
import jabaclass.settlement.infrastructure.batch.listener.SettlementRefundSkipListener;
import jabaclass.settlement.infrastructure.batch.processor.MonthlySettlementCreationItemProcessor;
import jabaclass.settlement.infrastructure.batch.processor.PaymentTargetCalculationItemProcessor;
import jabaclass.settlement.infrastructure.batch.processor.RefundTargetCalculationItemProcessor;
import jabaclass.settlement.infrastructure.batch.processor.SellerGradeCalculationItemProcessor;
import jabaclass.settlement.infrastructure.batch.listener.SettlementStepPhaseTimingListener;
import jabaclass.settlement.infrastructure.batch.listener.SettlementStepExecutionListener;
import jabaclass.settlement.infrastructure.batch.reader.MonthlySettlementCreationItemReader;
import jabaclass.settlement.infrastructure.batch.reader.PaymentTargetCalculationItemReader;
import jabaclass.settlement.infrastructure.batch.reader.RefundTargetCalculationItemReader;
import jabaclass.settlement.infrastructure.batch.reader.SellerGradeCalculationItemReader;
import jabaclass.settlement.infrastructure.batch.support.SettlementMonthResolver;
import jabaclass.settlement.infrastructure.batch.writer.MonthlySettlementItemWriter;
import jabaclass.settlement.infrastructure.batch.writer.SellerGradeItemWriter;
import jabaclass.settlement.infrastructure.batch.writer.SettlementTargetCalculationItemWriter;

@Configuration
public class SettlementCalculateJobConfig {

	private static final String SETTLEMENT_TARGET_QUERY = """
		select st
		from SettlementTarget st
		where st.settlementMonth = :settlementMonth
		  and st.targetType = :targetType
		  and st.calculationStatus = :calculationStatus
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
		Step settlementPaymentTargetCalculationStep,
		Step settlementRefundTargetCalculationStep,
		Step sellerGradeCalculationStep,
		Step monthlySettlementCreationStep,
		SettlementJobExecutionListener settlementJobExecutionListener
	) {
		return new JobBuilder("settlementCalculateJob", jobRepository)
			.listener(settlementJobExecutionListener)
			.start(settlementPaymentTargetCalculationStep)
			.next(settlementRefundTargetCalculationStep)
			.next(sellerGradeCalculationStep)
			.next(monthlySettlementCreationStep)
			.build();
	}

	@Bean
	public Step settlementPaymentTargetCalculationStep(
		JobRepository jobRepository,
		PlatformTransactionManager transactionManager,
		@Qualifier("settlementPaymentTargetItemReader") ItemStreamReader<PaymentTargetCalculationItem> settlementTargetItemReader,
		PaymentTargetCalculationItemProcessor paymentTargetCalculationItemProcessor,
		SettlementTargetCalculationItemWriter settlementTargetCalculationItemWriter,
		SettlementStepExecutionListener settlementStepExecutionListener,
		SettlementStepPhaseTimingListener settlementStepPhaseTimingListener,
		@Value("${settlement.batch.calculate.chunk-size:100}") int chunkSize
	) {
		return new StepBuilder("settlementPaymentTargetCalculationStep", jobRepository)
			.<PaymentTargetCalculationItem, SettlementTargetCalculationBatchItem>chunk(chunkSize)
			.transactionManager(transactionManager)
			.reader(settlementTargetItemReader)
			.processor(paymentTargetCalculationItemProcessor)
			.writer(settlementTargetCalculationItemWriter)
			.listener(settlementStepExecutionListener)
			.listener(settlementStepPhaseTimingListener)
			.build();
	}

	@Bean
	public Step settlementRefundTargetCalculationStep(
		JobRepository jobRepository,
		PlatformTransactionManager transactionManager,
		@Qualifier("settlementRefundTargetItemReader") ItemStreamReader<RefundTargetCalculationItem> settlementTargetItemReader,
		RefundTargetCalculationItemProcessor refundTargetCalculationItemProcessor,
		SettlementTargetCalculationItemWriter settlementTargetCalculationItemWriter,
		SettlementRefundSkipListener settlementRefundSkipListener,
		SettlementStepExecutionListener settlementStepExecutionListener,
		SettlementStepPhaseTimingListener settlementStepPhaseTimingListener,
		@Value("${settlement.batch.calculate.chunk-size:100}") int chunkSize
	) {
		return new StepBuilder("settlementRefundTargetCalculationStep", jobRepository)
			.<RefundTargetCalculationItem, SettlementTargetCalculationBatchItem>chunk(chunkSize)
			.transactionManager(transactionManager)
			.reader(settlementTargetItemReader)
			.processor(refundTargetCalculationItemProcessor)
			.writer(settlementTargetCalculationItemWriter)
			.faultTolerant()
			.skip(SettlementCalculationRetryableException.class)
			.skip(BusinessException.class)
			.skipLimit(Integer.MAX_VALUE)
			.listener(settlementRefundSkipListener)
			.listener(settlementStepExecutionListener)
			.listener(settlementStepPhaseTimingListener)
			.build();
	}

	@Bean
	@StepScope
	public ItemStreamReader<PaymentTargetCalculationItem> settlementPaymentTargetItemReader(
		EntityManagerFactory entityManagerFactory,
		SellerPromotionRepository sellerPromotionRepository,
		SettlementPromotionRepository settlementPromotionRepository,
		@Value("#{jobParameters['settlementMonth']}") String settlementMonthParam,
		@Value("${settlement.batch.calculate.chunk-size:100}") int chunkSize
	) {
		return new PaymentTargetCalculationItemReader(
			settlementTargetItemReader(
				entityManagerFactory,
				settlementMonthParam,
				SettlementTargetType.PAYMENT,
				"settlementPaymentTargetJpaItemReader",
				chunkSize
			),
			sellerPromotionRepository,
			settlementPromotionRepository,
			chunkSize
		);
	}

	@Bean
	@StepScope
	public ItemStreamReader<RefundTargetCalculationItem> settlementRefundTargetItemReader(
		EntityManagerFactory entityManagerFactory,
		SettlementTargetRepository settlementTargetRepository,
		SettlementTargetCalculationRepository settlementTargetCalculationRepository,
		SellerPromotionRepository sellerPromotionRepository,
		SettlementPromotionRepository settlementPromotionRepository,
		@Value("#{jobParameters['settlementMonth']}") String settlementMonthParam,
		@Value("${settlement.batch.calculate.chunk-size:100}") int chunkSize
	) {
		return new RefundTargetCalculationItemReader(
			settlementTargetItemReader(
				entityManagerFactory,
				settlementMonthParam,
				SettlementTargetType.REFUND,
				"settlementRefundTargetItemReader",
				chunkSize
			),
			settlementTargetRepository,
			settlementTargetCalculationRepository,
			sellerPromotionRepository,
			settlementPromotionRepository,
			chunkSize
		);
	}

	private JpaPagingItemReader<SettlementTarget> settlementTargetItemReader(
		EntityManagerFactory entityManagerFactory,
		String settlementMonthParam,
		SettlementTargetType targetType,
		String name,
		int chunkSize
	) {
		String settlementMonth = SettlementMonthResolver.resolve(settlementMonthParam);
		return new JpaPagingItemReaderBuilder<SettlementTarget>()
			.name(name)
			.entityManagerFactory(entityManagerFactory)
			.pageSize(chunkSize)
			.parameterValues(Map.of(
				"settlementMonth", settlementMonth,
				"targetType", targetType,
				"calculationStatus", SettlementTargetCalculationStatus.PENDING
			))
			.queryString(SETTLEMENT_TARGET_QUERY)
			.build();
	}

	@Bean
	public Step sellerGradeCalculationStep(
		JobRepository jobRepository,
		PlatformTransactionManager transactionManager,
		ItemStreamReader<SellerGradeCalculationItem> sellerGradeCalculationReader,
		ItemProcessor<SellerGradeCalculationItem, SellerGrade> sellerGradeCalculationProcessor,
		SellerGradeItemWriter sellerGradeItemWriter,
		SettlementStepExecutionListener settlementStepExecutionListener,
		SettlementStepPhaseTimingListener settlementStepPhaseTimingListener,
		@Value("${settlement.batch.calculate.chunk-size:100}") int chunkSize
	) {
		return new StepBuilder("sellerGradeCalculationStep", jobRepository)
			.<SellerGradeCalculationItem, SellerGrade>chunk(chunkSize)
			.transactionManager(transactionManager)
			.reader(sellerGradeCalculationReader)
			.processor(sellerGradeCalculationProcessor)
			.writer(sellerGradeItemWriter)
			.listener(settlementStepExecutionListener)
			.listener(settlementStepPhaseTimingListener)
			.build();
	}

	@Bean
	public Step monthlySettlementCreationStep(
		JobRepository jobRepository,
		PlatformTransactionManager transactionManager,
		ItemStreamReader<MonthlySettlementCreationItem> monthlySettlementCreationReader,
		ItemProcessor<MonthlySettlementCreationItem, Settlement> monthlySettlementCreationProcessor,
		MonthlySettlementItemWriter monthlySettlementItemWriter,
		SettlementStepExecutionListener settlementStepExecutionListener,
		SettlementStepPhaseTimingListener settlementStepPhaseTimingListener,
		@Value("${settlement.batch.calculate.chunk-size:100}") int chunkSize
	) {
		return new StepBuilder("monthlySettlementCreationStep", jobRepository)
			.<MonthlySettlementCreationItem, Settlement>chunk(chunkSize)
			.transactionManager(transactionManager)
			.reader(monthlySettlementCreationReader)
			.processor(monthlySettlementCreationProcessor)
			.writer(monthlySettlementItemWriter)
			.listener(settlementStepExecutionListener)
			.listener(settlementStepPhaseTimingListener)
			.build();
	}

	@Bean
	@StepScope
	public JpaPagingItemReader<SettlementTargetSummary> sellerGradeCalculationSummaryReader(
		EntityManagerFactory entityManagerFactory,
		@Value("#{jobParameters['settlementMonth']}") String settlementMonthParam,
		@Value("${settlement.batch.calculate.chunk-size:100}") int chunkSize
	) {
		String settlementMonth = SettlementMonthResolver.resolve(settlementMonthParam);
		return new JpaPagingItemReaderBuilder<SettlementTargetSummary>()
			.name("sellerGradeCalculationSummaryReader")
			.entityManagerFactory(entityManagerFactory)
			.pageSize(chunkSize)
			.parameterValues(Map.of("settlementMonth", settlementMonth))
			.queryString(SETTLEMENT_AGGREGATION_QUERY)
			.build();
	}

	@Bean
	@StepScope
	public JpaPagingItemReader<SettlementTargetSummary> monthlySettlementCreationSummaryReader(
		EntityManagerFactory entityManagerFactory,
		@Value("#{jobParameters['settlementMonth']}") String settlementMonthParam,
		@Value("${settlement.batch.calculate.chunk-size:100}") int chunkSize
	) {
		String settlementMonth = SettlementMonthResolver.resolve(settlementMonthParam);
		return new JpaPagingItemReaderBuilder<SettlementTargetSummary>()
			.name("monthlySettlementCreationSummaryReader")
			.entityManagerFactory(entityManagerFactory)
			.pageSize(chunkSize)
			.parameterValues(Map.of("settlementMonth", settlementMonth))
			.queryString(SETTLEMENT_AGGREGATION_QUERY)
			.build();
	}

	@Bean
	@StepScope
	public ItemStreamReader<SellerGradeCalculationItem> sellerGradeCalculationReader(
		JpaPagingItemReader<SettlementTargetSummary> sellerGradeCalculationSummaryReader,
		SettlementTargetCalculationRepository settlementTargetCalculationRepository,
		SellerGradeRepository sellerGradeRepository,
		@Value("#{jobParameters['settlementMonth']}") String settlementMonthParam,
		@Value("${settlement.batch.calculate.chunk-size:100}") int chunkSize
	) {
		String settlementMonth = SettlementMonthResolver.resolve(settlementMonthParam);
		return new SellerGradeCalculationItemReader(
			sellerGradeCalculationSummaryReader,
			settlementTargetCalculationRepository,
			sellerGradeRepository,
			settlementMonth,
			chunkSize
		);
	}

	@Bean
	@StepScope
	public ItemStreamReader<MonthlySettlementCreationItem> monthlySettlementCreationReader(
		JpaPagingItemReader<SettlementTargetSummary> monthlySettlementCreationSummaryReader,
		SettlementRepository settlementRepository,
		SettlementTargetCalculationRepository settlementTargetCalculationRepository,
		SellerGradeRepository sellerGradeRepository,
		@Value("#{jobParameters['settlementMonth']}") String settlementMonthParam,
		@Value("${settlement.batch.calculate.chunk-size:100}") int chunkSize
	) {
		String settlementMonth = SettlementMonthResolver.resolve(settlementMonthParam);
		return new MonthlySettlementCreationItemReader(
			monthlySettlementCreationSummaryReader,
			settlementRepository,
			settlementTargetCalculationRepository,
			sellerGradeRepository,
			settlementMonth,
			chunkSize
		);
	}

	@Bean
	@StepScope
	public ItemProcessor<SellerGradeCalculationItem, SellerGrade> sellerGradeCalculationProcessor(
		SettlementCalculateService settlementCalculateService
	) {
		List<SellerGradePolicy> activeSellerGradePolicies = settlementCalculateService.findActiveSellerGradePolicies();
		return new SellerGradeCalculationItemProcessor(settlementCalculateService, activeSellerGradePolicies);
	}

	@Bean
	@StepScope
	public ItemProcessor<MonthlySettlementCreationItem, Settlement> monthlySettlementCreationProcessor(
		SettlementCalculateService settlementCalculateService
	) {
		List<SellerGradePolicy> activeSellerGradePolicies = settlementCalculateService.findActiveSellerGradePolicies();
		return new MonthlySettlementCreationItemProcessor(settlementCalculateService, activeSellerGradePolicies);
	}

	@Bean
	@StepScope
	public SellerGradeItemWriter sellerGradeItemWriter(
		SellerGradeRepository sellerGradeRepository
	) {
		return new SellerGradeItemWriter(sellerGradeRepository);
	}

	@Bean
	@StepScope
	public MonthlySettlementItemWriter monthlySettlementItemWriter(
		SettlementRepository settlementRepository
	) {
		return new MonthlySettlementItemWriter(settlementRepository);
	}
}
