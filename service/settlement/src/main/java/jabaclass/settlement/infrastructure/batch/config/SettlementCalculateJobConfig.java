package jabaclass.settlement.infrastructure.batch.config;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import jakarta.persistence.EntityManagerFactory;
import javax.sql.DataSource;

import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.infrastructure.item.ItemProcessor;
import org.springframework.batch.infrastructure.item.ItemStreamReader;
import org.springframework.batch.infrastructure.item.database.JdbcCursorItemReader;
import org.springframework.batch.infrastructure.item.database.builder.JdbcCursorItemReaderBuilder;
import org.springframework.batch.infrastructure.item.database.JpaCursorItemReader;
import org.springframework.batch.infrastructure.item.database.builder.JpaCursorItemReaderBuilder;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import jabaclass.settlement.application.dto.MonthlySettlementCreationItem;
import jabaclass.settlement.application.dto.SellerGradeCalculationItem;
import jabaclass.settlement.application.dto.SettlementTargetInfo;
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
import jabaclass.settlement.infrastructure.batch.reader.MonthlySettlementCreationSummaryItemReader;
import jabaclass.settlement.infrastructure.batch.reader.PaymentTargetCalculationItemReader;
import jabaclass.settlement.infrastructure.batch.reader.RefundTargetCalculationItemReader;
import jabaclass.settlement.infrastructure.batch.reader.SellerGradeCalculationItemReader;
import jabaclass.settlement.infrastructure.batch.support.SettlementMonthResolver;
import jabaclass.settlement.infrastructure.batch.writer.MonthlySettlementItemWriter;
import jabaclass.settlement.infrastructure.batch.writer.SellerGradeItemWriter;
import jabaclass.settlement.infrastructure.batch.writer.SettlementTargetCalculationItemWriter;

@Configuration
public class SettlementCalculateJobConfig {

	private static final String SETTLEMENT_AGGREGATION_CURSOR_QUERY = """
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

	private static final String TARGET_CURSOR_QUERY = """
		select
			id,
			settlement_month,
			seller_id,
			order_id,
			payment_id,
			refund_id,
			product_id,
			target_type,
			settlement_base_amount,
			occurred_at
		from settlement_targets
		where settlement_month = ?
		  and target_type = ?
		  and calculation_status = ?
		order by occurred_at, id
		""";

	private static final String SELLER_ID_CURSOR_QUERY = """
		select distinct stc.sellerId
		from SettlementTargetCalculation stc
		where stc.settlementMonth = :settlementMonth
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
		DataSource dataSource,
		SellerPromotionRepository sellerPromotionRepository,
		SettlementPromotionRepository settlementPromotionRepository,
		@Value("#{jobParameters['settlementMonth']}") String settlementMonthParam,
		@Value("${settlement.batch.calculate.chunk-size:100}") int chunkSize
	) {
		String settlementMonth = SettlementMonthResolver.resolve(settlementMonthParam);
		return new PaymentTargetCalculationItemReader(
			settlementTargetCursorReader(
				dataSource,
				"settlementPaymentTargetCursorReader",
				settlementMonth,
				SettlementTargetType.PAYMENT
			),
			sellerPromotionRepository,
			settlementPromotionRepository,
			chunkSize
		);
	}

	@Bean
	@StepScope
	public ItemStreamReader<RefundTargetCalculationItem> settlementRefundTargetItemReader(
		DataSource dataSource,
		SettlementTargetRepository settlementTargetRepository,
		SettlementTargetCalculationRepository settlementTargetCalculationRepository,
		SellerPromotionRepository sellerPromotionRepository,
		SettlementPromotionRepository settlementPromotionRepository,
		@Value("#{jobParameters['settlementMonth']}") String settlementMonthParam,
		@Value("${settlement.batch.calculate.chunk-size:100}") int chunkSize
	) {
		String settlementMonth = SettlementMonthResolver.resolve(settlementMonthParam);
		return new RefundTargetCalculationItemReader(
			settlementTargetCursorReader(
				dataSource,
				"settlementRefundTargetCursorReader",
				settlementMonth,
				SettlementTargetType.REFUND
			),
			settlementTargetRepository,
			settlementTargetCalculationRepository,
			sellerPromotionRepository,
			settlementPromotionRepository,
			chunkSize
		);
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
	public JpaCursorItemReader<UUID> sellerGradeCalculationSellerReader(
		EntityManagerFactory entityManagerFactory,
		@Value("#{jobParameters['settlementMonth']}") String settlementMonthParam
	) {
		String settlementMonth = SettlementMonthResolver.resolve(settlementMonthParam);
		return settlementTargetCalculationSellerCursorReader(
			entityManagerFactory,
			"sellerGradeCalculationSellerReader",
			settlementMonth
		);
	}

	private JdbcCursorItemReader<SettlementTargetInfo> settlementTargetCursorReader(
		DataSource dataSource,
		String readerName,
		String settlementMonth,
		SettlementTargetType targetType
	) {
		return new JdbcCursorItemReaderBuilder<SettlementTargetInfo>()
			.name(readerName)
			.dataSource(dataSource)
			.sql(TARGET_CURSOR_QUERY)
			.preparedStatementSetter(ps -> {
				ps.setString(1, settlementMonth);
				ps.setString(2, targetType.name());
				ps.setString(3, SettlementTargetCalculationStatus.PENDING.name());
			})
			.rowMapper((rs, rowNum) -> new SettlementTargetInfo(
				rs.getObject("id", UUID.class),
				rs.getString("settlement_month"),
				rs.getObject("seller_id", UUID.class),
				rs.getObject("order_id", UUID.class),
				rs.getObject("payment_id", UUID.class),
				rs.getObject("refund_id", UUID.class),
				rs.getObject("product_id", UUID.class),
				SettlementTargetType.valueOf(rs.getString("target_type")),
				rs.getBigDecimal("settlement_base_amount"),
				rs.getTimestamp("occurred_at").toLocalDateTime()
			))
			.saveState(true)
			.build();
	}

	private JpaCursorItemReader<UUID> settlementTargetCalculationSellerCursorReader(
		EntityManagerFactory entityManagerFactory,
		String readerName,
		String settlementMonth
	) {
		return new JpaCursorItemReaderBuilder<UUID>()
			.name(readerName)
			.entityManagerFactory(entityManagerFactory)
			.parameterValues(Map.of("settlementMonth", settlementMonth))
			.queryString(SELLER_ID_CURSOR_QUERY)
			.saveState(true)
			.build();
	}

	private JpaCursorItemReader<SettlementTargetSummary> settlementTargetSummaryCursorReader(
		EntityManagerFactory entityManagerFactory,
		String readerName,
		String settlementMonth
	) {
		return new JpaCursorItemReaderBuilder<SettlementTargetSummary>()
			.name(readerName)
			.entityManagerFactory(entityManagerFactory)
			.parameterValues(Map.of("settlementMonth", settlementMonth))
			.queryString(SETTLEMENT_AGGREGATION_CURSOR_QUERY)
			.saveState(true)
			.build();
	}

	@Bean
	@StepScope
	public JpaCursorItemReader<SettlementTargetSummary> monthlySettlementCreationSummaryReader(
		EntityManagerFactory entityManagerFactory,
		@Value("#{jobParameters['settlementMonth']}") String settlementMonthParam
	) {
		String settlementMonth = SettlementMonthResolver.resolve(settlementMonthParam);
		return settlementTargetSummaryCursorReader(
			entityManagerFactory,
			"monthlySettlementCreationSummaryReader",
			settlementMonth
		);
	}

	@Bean
	@StepScope
	public ItemStreamReader<SellerGradeCalculationItem> sellerGradeCalculationReader(
		JpaCursorItemReader<UUID> sellerGradeCalculationSellerReader,
		SettlementTargetCalculationRepository settlementTargetCalculationRepository,
		SellerGradeRepository sellerGradeRepository,
		@Value("#{jobParameters['settlementMonth']}") String settlementMonthParam,
		@Value("${settlement.batch.calculate.chunk-size:100}") int chunkSize
	) {
		String settlementMonth = SettlementMonthResolver.resolve(settlementMonthParam);
		return new SellerGradeCalculationItemReader(
			sellerGradeCalculationSellerReader,
			settlementTargetCalculationRepository,
			sellerGradeRepository,
			settlementMonth,
			chunkSize
		);
	}

	@Bean
	@StepScope
	public ItemStreamReader<MonthlySettlementCreationItem> monthlySettlementCreationReader(
		JpaCursorItemReader<SettlementTargetSummary> monthlySettlementCreationSummaryReader,
		SettlementRepository settlementRepository,
		SettlementTargetCalculationRepository settlementTargetCalculationRepository,
		SellerGradeRepository sellerGradeRepository,
		@Value("#{jobParameters['settlementMonth']}") String settlementMonthParam,
		@Value("${settlement.batch.calculate.chunk-size:100}") int chunkSize
	) {
		String settlementMonth = SettlementMonthResolver.resolve(settlementMonthParam);
		return new MonthlySettlementCreationSummaryItemReader(
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
