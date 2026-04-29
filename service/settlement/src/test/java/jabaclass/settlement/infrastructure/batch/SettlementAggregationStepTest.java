package jabaclass.settlement.infrastructure.batch;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator.ReplaceUnderscores;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.JobInstance;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.ListableStepLocator;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import jabaclass.settlement.domain.model.grade.SellerGradePolicy;
import jabaclass.settlement.domain.model.grade.SellerGradeType;
import jabaclass.settlement.domain.model.settlement.Settlement;
import jabaclass.settlement.domain.model.settlement.SettlementStatus;
import jabaclass.settlement.domain.model.settlement.SettlementTarget;
import jabaclass.settlement.domain.model.settlement.SettlementTargetCalculation;
import jabaclass.settlement.infrastructure.persistence.SellerGradeJpaRepository;
import jabaclass.settlement.infrastructure.persistence.SellerGradePolicyJpaRepository;
import jabaclass.settlement.infrastructure.persistence.SettlementJpaRepository;
import jabaclass.settlement.infrastructure.persistence.SettlementTargetCalculationJpaRepository;
import jabaclass.settlement.infrastructure.persistence.SettlementTargetJpaRepository;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@DirtiesContext
@DisplayNameGeneration(ReplaceUnderscores.class)
@SuppressWarnings("NonAsciiCharacters")
class SettlementAggregationStepTest {

	@Autowired
	private JobRepository jobRepository;

	@Autowired
	private Job settlementCalculateJob;

	@Autowired
	private SettlementTargetJpaRepository settlementTargetJpaRepository;

	@Autowired
	private SettlementTargetCalculationJpaRepository settlementTargetCalculationJpaRepository;

	@Autowired
	private SettlementJpaRepository settlementJpaRepository;

	@Autowired
	private SellerGradeJpaRepository sellerGradeJpaRepository;

	@Autowired
	private SellerGradePolicyJpaRepository sellerGradePolicyJpaRepository;

	@BeforeEach
	void setUp() {
		sellerGradeJpaRepository.deleteAllInBatch();
		settlementJpaRepository.deleteAllInBatch();
		settlementTargetCalculationJpaRepository.deleteAllInBatch();
		settlementTargetJpaRepository.deleteAllInBatch();
		sellerGradePolicyJpaRepository.deleteAllInBatch();
	}

	@Test
	void sellerGrade와_monthlySettlement_step은_조립한_입력으로_등급과_정산을_생성한다() throws Exception {
		String settlementMonth = "2026-03";
		UUID sellerId = UUID.randomUUID();
		SellerGradePolicy policy = saveBasicPolicy();

		SettlementTarget currentTarget = settlementTargetJpaRepository.save(SettlementTarget.forPayment(
			UUID.randomUUID(),
			settlementMonth,
			sellerId,
			UUID.randomUUID(),
			UUID.randomUUID(),
			UUID.randomUUID(),
			new BigDecimal("10000.00"),
			LocalDateTime.of(2026, 3, 10, 10, 0)
		));
		SettlementTarget previousTarget = settlementTargetJpaRepository.save(SettlementTarget.forPayment(
			UUID.randomUUID(),
			"2026-02",
			sellerId,
			UUID.randomUUID(),
			UUID.randomUUID(),
			UUID.randomUUID(),
			new BigDecimal("5000.00"),
			LocalDateTime.of(2026, 2, 10, 10, 0)
		));
		settlementTargetCalculationJpaRepository.save(SettlementTargetCalculation.forPayment(currentTarget, null, null, null));
		settlementTargetCalculationJpaRepository.save(SettlementTargetCalculation.forPayment(previousTarget, null, null, null));

		StepExecution sellerGradeStepExecution = executeStep("sellerGradeCalculationStep", jobParameters(settlementMonth));
		StepExecution monthlySettlementStepExecution = executeStep("monthlySettlementCreationStep", jobParameters(settlementMonth));

		assertThat(sellerGradeStepExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
		assertThat(sellerGradeStepExecution.getReadCount()).isEqualTo(1);
		assertThat(sellerGradeStepExecution.getWriteCount()).isEqualTo(1);

		assertThat(monthlySettlementStepExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
		assertThat(monthlySettlementStepExecution.getReadCount()).isEqualTo(1);
		assertThat(monthlySettlementStepExecution.getWriteCount()).isEqualTo(1);
		assertThat(monthlySettlementStepExecution.getFilterCount()).isZero();

		assertThat(settlementJpaRepository.findAll()).singleElement().satisfies(settlement -> {
			assertThat(settlement.getSellerId()).isEqualTo(sellerId);
			assertThat(settlement.getSettlementMonth()).isEqualTo(settlementMonth);
			assertThat(settlement.getOriginalAmount()).isEqualByComparingTo("10000.00");
			assertThat(settlement.getGradeBaseAmount()).isEqualByComparingTo("15000.00");
			assertThat(settlement.getSellerGradeCode()).isEqualTo(SellerGradeType.BASIC);
			assertThat(settlement.getSellerGradePolicyId()).isEqualTo(policy.getId());
			assertThat(settlement.getFeeAmount()).isEqualByComparingTo("330.00");
			assertThat(settlement.getSettlementAmount()).isEqualByComparingTo("9670.00");
			assertThat(settlement.getStatus()).isEqualTo(SettlementStatus.READY);
		});

		assertThat(sellerGradeJpaRepository.findAll()).singleElement().satisfies(sellerGrade -> {
			assertThat(sellerGrade.getSellerId()).isEqualTo(sellerId);
			assertThat(sellerGrade.getSellerGradePolicyId()).isEqualTo(policy.getId());
			assertThat(sellerGrade.getCalculatedMonth()).isEqualTo(settlementMonth);
		});
	}

	@Test
	void monthlySettlementStep은_재집계_불가한_정산은_processor에서_필터링한다() throws Exception {
		String settlementMonth = "2026-03";
		UUID sellerId = UUID.randomUUID();
		SellerGradePolicy policy = saveBasicPolicy();

		Settlement existingSettlement = Settlement.createReady(
			sellerId,
			settlementMonth,
			new BigDecimal("1000.00"),
			SellerGradeType.BASIC,
			policy.getId(),
			new BigDecimal("1000.00"),
			new BigDecimal("33.00"),
			new BigDecimal("0.0330"),
			new BigDecimal("967.00")
		);
		existingSettlement.markSent(LocalDateTime.of(2026, 3, 25, 12, 0));
		settlementJpaRepository.save(existingSettlement);

		SettlementTarget currentTarget = settlementTargetJpaRepository.save(SettlementTarget.forPayment(
			UUID.randomUUID(),
			settlementMonth,
			sellerId,
			UUID.randomUUID(),
			UUID.randomUUID(),
			UUID.randomUUID(),
			new BigDecimal("10000.00"),
			LocalDateTime.of(2026, 3, 10, 10, 0)
		));
		settlementTargetCalculationJpaRepository.save(SettlementTargetCalculation.forPayment(currentTarget, null, null, null));

		StepExecution sellerGradeStepExecution = executeStep("sellerGradeCalculationStep", jobParameters(settlementMonth));
		StepExecution monthlySettlementStepExecution = executeStep("monthlySettlementCreationStep", jobParameters(settlementMonth));

		assertThat(sellerGradeStepExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
		assertThat(sellerGradeStepExecution.getWriteCount()).isEqualTo(1);

		assertThat(monthlySettlementStepExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
		assertThat(monthlySettlementStepExecution.getReadCount()).isEqualTo(1);
		assertThat(monthlySettlementStepExecution.getWriteCount()).isZero();
		assertThat(monthlySettlementStepExecution.getFilterCount()).isEqualTo(1);

		assertThat(settlementJpaRepository.findAll()).hasSize(1);
		assertThat(sellerGradeJpaRepository.findAll()).hasSize(1);
	}

	private StepExecution executeStep(String stepName, JobParameters jobParameters) throws Exception {
		JobInstance jobInstance = jobRepository.createJobInstance(settlementCalculateJob.getName(), jobParameters);
		JobExecution jobExecution = jobRepository.createJobExecution(
			jobInstance,
			jobParameters,
			new org.springframework.batch.infrastructure.item.ExecutionContext()
		);
		Step step = ((ListableStepLocator) settlementCalculateJob).getStep(stepName);
		StepExecution stepExecution = jobRepository.createStepExecution(step.getName(), jobExecution);
		step.execute(stepExecution);
		return stepExecution;
	}

	private SellerGradePolicy saveBasicPolicy() {
		return sellerGradePolicyJpaRepository.save(new SellerGradePolicy(
			SellerGradeType.BASIC,
			1,
			BigDecimal.ZERO,
			null,
			new BigDecimal("0.0330"),
			true,
			LocalDateTime.of(2026, 1, 1, 0, 0),
			null
		));
	}

	private JobParameters jobParameters(String settlementMonth) {
		return new JobParametersBuilder()
			.addString("settlementMonth", settlementMonth)
			.addLong("requestedAt", System.currentTimeMillis())
			.toJobParameters();
	}
}
