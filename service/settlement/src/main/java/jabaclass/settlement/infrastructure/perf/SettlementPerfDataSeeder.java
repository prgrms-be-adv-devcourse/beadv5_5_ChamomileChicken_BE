package jabaclass.settlement.infrastructure.perf;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@Profile("dev-local")
@Order(Ordered.LOWEST_PRECEDENCE)
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "settlement.perf.seed", name = "enabled", havingValue = "true")
public class SettlementPerfDataSeeder implements ApplicationRunner {

	private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");
	private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2, RoundingMode.DOWN);
	private static final String PAYMENT = "PAYMENT";
	private static final String REFUND = "REFUND";
	private static final String CALCULATED = "CALCULATED";
	private static final String READY = "READY";
	private static final String HOLD = "HOLD";
	private static final String FAILED = "FAILED";
	private static final String TRANSFERRING = "TRANSFERRING";
	private static final String SENT = "SENT";

	private final JdbcTemplate jdbcTemplate;
	private final SettlementPerfSeedProperties properties;

	@Override
	public void run(ApplicationArguments args) {
		validateProperties();

		if (properties.isTruncateBeforeSeed()) {
			truncateSeedTables();
		}

		List<GradePolicyRow> gradePolicies = loadGradePolicies();
		PromotionRow promotion = loadNewSellerPromotion();

		log.info(
			"[SETTLEMENT_PERF_SEED] start sellerCount={} targetMonthPaymentCount={} previousMonthPaymentCount={} twoMonthsAgoPaymentCount={} refundRatio={} promotedSellerRatio={} existingSettlementRatio={} targetMonth={} batchSize={}",
			properties.getSellerCount(),
			properties.getTargetMonthPaymentCount(),
			properties.getPreviousMonthPaymentCount(),
			properties.getTwoMonthsAgoPaymentCount(),
			properties.getRefundRatio(),
			properties.getPromotedSellerRatio(),
			properties.getExistingSettlementRatio(),
			properties.getTargetMonth(),
			properties.getBatchSize()
		);

		seedPerfData(gradePolicies, promotion);

		log.info("[SETTLEMENT_PERF_SEED] completed");
	}

	private void validateProperties() {
		if (properties.getSellerCount() <= 0) {
			throw new IllegalArgumentException("sellerCount는 1 이상이어야 합니다.");
		}
		if (properties.getTargetMonthPaymentCount() <= 0) {
			throw new IllegalArgumentException("targetMonthPaymentCount는 1 이상이어야 합니다.");
		}
		if (properties.getPreviousMonthPaymentCount() < 0) {
			throw new IllegalArgumentException("previousMonthPaymentCount는 0 이상이어야 합니다.");
		}
		if (properties.getTwoMonthsAgoPaymentCount() < 0) {
			throw new IllegalArgumentException("twoMonthsAgoPaymentCount는 0 이상이어야 합니다.");
		}
		if (properties.getBatchSize() <= 0) {
			throw new IllegalArgumentException("batchSize는 1 이상이어야 합니다.");
		}
		if (properties.getRefundRatio() < 0 || properties.getRefundRatio() > 1) {
			throw new IllegalArgumentException("refundRatio는 0 이상 1 이하여야 합니다.");
		}
		if (properties.getPromotedSellerRatio() < 0 || properties.getPromotedSellerRatio() > 1) {
			throw new IllegalArgumentException("promotedSellerRatio는 0 이상 1 이하여야 합니다.");
		}
		if (properties.getExistingSettlementRatio() < 0 || properties.getExistingSettlementRatio() > 1) {
			throw new IllegalArgumentException("existingSettlementRatio는 0 이상 1 이하여야 합니다.");
		}

		YearMonth.parse(properties.getTargetMonth());
	}

	private void truncateSeedTables() {
		log.info("[SETTLEMENT_PERF_SEED] truncate existing settlement tables");
		jdbcTemplate.execute("""
			TRUNCATE TABLE
				settlement_transfers,
				settlements,
				seller_grades,
				seller_promotions,
				settlement_target_calculations,
				settlement_targets
			CASCADE
			""");
	}

	private List<GradePolicyRow> loadGradePolicies() {
		List<GradePolicyRow> policies = jdbcTemplate.query(
			"""
				SELECT id, grade_code, min_sales_amount, max_sales_amount, fee_rate
				FROM seller_grade_policies
				WHERE active = true
				ORDER BY min_sales_amount ASC
				""",
			(rs, rowNum) -> new GradePolicyRow(
				rs.getObject("id", UUID.class),
				rs.getString("grade_code"),
				rs.getBigDecimal("min_sales_amount"),
				rs.getBigDecimal("max_sales_amount"),
				rs.getBigDecimal("fee_rate")
			)
		);

		if (policies.isEmpty()) {
			throw new IllegalStateException("활성 seller_grade_policies가 없습니다. dev 프로필 초기화 데이터를 먼저 준비해주세요.");
		}

		return policies;
	}

	private PromotionRow loadNewSellerPromotion() {
		List<PromotionRow> promotions = jdbcTemplate.query(
			"""
				SELECT id, fee_rate, duration_days
				FROM settlement_promotions
				WHERE promotion_type = 'NEW_SELLER'
				  AND active = true
				ORDER BY created_at ASC
				LIMIT 1
				""",
			(rs, rowNum) -> new PromotionRow(
				rs.getObject("id", UUID.class),
				rs.getBigDecimal("fee_rate"),
				rs.getInt("duration_days")
			)
		);

		return promotions.isEmpty() ? null : promotions.get(0);
	}

	private void seedPerfData(List<GradePolicyRow> gradePolicies, PromotionRow promotion) {
		Random random = new Random(properties.getRandomSeed());
		YearMonth targetMonth = YearMonth.parse(properties.getTargetMonth());
		YearMonth previousMonth = targetMonth.minusMonths(1);
		YearMonth twoMonthsAgo = targetMonth.minusMonths(2);
		int targetMonthPaymentCount = properties.getTargetMonthPaymentCount();
		int previousMonthPaymentCount = properties.getPreviousMonthPaymentCount();
		int twoMonthsAgoPaymentCount = properties.getTwoMonthsAgoPaymentCount();
		int totalPaymentTargetCount = targetMonthPaymentCount + previousMonthPaymentCount + twoMonthsAgoPaymentCount;
		LocalDateTime now = LocalDateTime.now();

		List<UUID> sellerIds = new ArrayList<>(properties.getSellerCount());
		boolean[] promotedSellerFlags = new boolean[properties.getSellerCount()];
		long[] recentThreeMonthSalesCents = new long[properties.getSellerCount()];
		long[] targetMonthSalesCents = new long[properties.getSellerCount()];

		for (int sellerIndex = 0; sellerIndex < properties.getSellerCount(); sellerIndex++) {
			sellerIds.add(deterministicUuid("seller", sellerIndex));
			promotedSellerFlags[sellerIndex] = promotion != null
				&& random.nextDouble() < properties.getPromotedSellerRatio();
		}

		LocalDateTime promotionStartedAt = targetMonth.atDay(1).atStartOfDay();
		LocalDateTime promotionEndedAt = promotion == null
			? null
			: promotionStartedAt.plusDays(promotion.durationDays()).minusNanos(1);

		List<SellerPromotionSeedRow> sellerPromotionRows = new ArrayList<>();
		if (promotion != null) {
			for (int sellerIndex = 0; sellerIndex < properties.getSellerCount(); sellerIndex++) {
				if (!promotedSellerFlags[sellerIndex]) {
					continue;
				}

				sellerPromotionRows.add(new SellerPromotionSeedRow(
					deterministicUuid("seller-promotion", sellerIndex),
					now,
					now,
					sellerIds.get(sellerIndex),
					promotion.id(),
					promotionStartedAt,
					promotionEndedAt,
					true
				));
			}

			insertSellerPromotionRows(sellerPromotionRows, properties.getBatchSize());
			log.info("[SETTLEMENT_PERF_SEED] seeded seller_promotions count={}", sellerPromotionRows.size());
		}

		List<SettlementTargetSeedRow> targetRows = new ArrayList<>(properties.getBatchSize());
		List<SettlementTargetCalculationSeedRow> calculationRows = new ArrayList<>(properties.getBatchSize());
		long generatedRefundCount = 0L;

		for (int paymentIndex = 0; paymentIndex < totalPaymentTargetCount; paymentIndex++) {
			int sellerIndex = pickSellerIndex(random, properties.getSellerCount());
			UUID sellerId = sellerIds.get(sellerIndex);
			YearMonth paymentMonth = resolvePaymentMonth(
				paymentIndex,
				targetMonthPaymentCount,
				previousMonthPaymentCount,
				targetMonth,
				previousMonth,
				twoMonthsAgo
			);
			LocalDateTime paymentOccurredAt = randomDateTimeInMonth(random, paymentMonth);
			long paymentAmountCents = generatePaymentAmountCents(random);

			UUID orderId = deterministicUuid("order", paymentIndex);
			UUID paymentId = deterministicUuid("payment", paymentIndex);
			UUID productId = deterministicUuid("product", sellerIndex + ":" + random.nextInt(50));
			UUID paymentTargetId = deterministicUuid("settlement-target-payment", paymentIndex);
			UUID paymentSourceEventId = deterministicUuid("settlement-source-payment", paymentIndex);
			UUID paymentCalculationId = deterministicUuid("settlement-calculation-payment", paymentIndex);

			BigDecimal paymentAmount = amount(paymentAmountCents);
			PromotionApplication appliedPromotion = resolvePromotion(
				promotion,
				promotedSellerFlags[sellerIndex],
				paymentOccurredAt,
				promotionStartedAt,
				promotionEndedAt
			);
			LocalDateTime paymentRequestedAt = paymentOccurredAt.plusSeconds(10);
			LocalDateTime paymentCompletedAt = paymentOccurredAt.plusSeconds(30);

			targetRows.add(new SettlementTargetSeedRow(
				paymentTargetId,
				now,
				now,
				paymentSourceEventId,
				paymentMonth.toString(),
				sellerId,
				orderId,
				paymentId,
				null,
				productId,
				PAYMENT,
				paymentAmount,
				paymentOccurredAt,
				CALCULATED,
				paymentRequestedAt,
				paymentCompletedAt,
				null
			));

			calculationRows.add(new SettlementTargetCalculationSeedRow(
				paymentCalculationId,
				now,
				now,
				paymentTargetId,
				paymentMonth.toString(),
				sellerId,
				paymentAmount,
				appliedPromotion.promotionId(),
				appliedPromotion.promotionType(),
				appliedPromotion.feeRate(),
				null,
				paymentCompletedAt
			));

			accumulateSales(recentThreeMonthSalesCents, sellerIndex, paymentMonth, paymentAmountCents, targetMonth, previousMonth,
				twoMonthsAgo);
			accumulateTargetMonthSales(targetMonthSalesCents, sellerIndex, paymentMonth, paymentAmountCents, targetMonth);

			if (random.nextDouble() < properties.getRefundRatio()) {
				generatedRefundCount++;
				long refundAmountCents = generateRefundAmountCents(random, paymentAmountCents);
				LocalDateTime refundOccurredAt = paymentOccurredAt
					.plusHours(1 + random.nextInt(72))
					.plusDays(random.nextInt(12));
				YearMonth refundMonth = YearMonth.from(refundOccurredAt);

				UUID refundId = deterministicUuid("refund", paymentIndex);
				UUID refundTargetId = deterministicUuid("settlement-target-refund", paymentIndex);
				UUID refundSourceEventId = deterministicUuid("settlement-source-refund", paymentIndex);
				UUID refundCalculationId = deterministicUuid("settlement-calculation-refund", paymentIndex);
				BigDecimal refundAmount = amount(refundAmountCents).negate();
				LocalDateTime refundRequestedAt = refundOccurredAt.plusSeconds(10);
				LocalDateTime refundCompletedAt = refundOccurredAt.plusSeconds(30);

				targetRows.add(new SettlementTargetSeedRow(
					refundTargetId,
					now,
					now,
					refundSourceEventId,
					refundMonth.toString(),
					sellerId,
					orderId,
					paymentId,
					refundId,
					productId,
					REFUND,
					refundAmount,
					refundOccurredAt,
					CALCULATED,
					refundRequestedAt,
					refundCompletedAt,
					null
				));

				calculationRows.add(new SettlementTargetCalculationSeedRow(
					refundCalculationId,
					now,
					now,
					refundTargetId,
					refundMonth.toString(),
					sellerId,
					refundAmount,
					appliedPromotion.promotionId(),
					appliedPromotion.promotionType(),
					appliedPromotion.feeRate(),
					paymentCalculationId,
					refundCompletedAt
				));

				accumulateSales(recentThreeMonthSalesCents, sellerIndex, refundMonth, -refundAmountCents, targetMonth, previousMonth,
					twoMonthsAgo);
				accumulateTargetMonthSales(targetMonthSalesCents, sellerIndex, refundMonth, -refundAmountCents, targetMonth);
			}

			if (targetRows.size() >= properties.getBatchSize()) {
				insertSettlementTargetRows(targetRows);
				targetRows.clear();
			}

			if (calculationRows.size() >= properties.getBatchSize()) {
				insertSettlementTargetCalculationRows(calculationRows);
				calculationRows.clear();
			}

			if ((paymentIndex + 1) % 100_000 == 0) {
				log.info(
					"[SETTLEMENT_PERF_SEED] progress paymentTargets={} refundTargets={} targetRowsInsertedSoFar={} calculationRowsInsertedSoFar={}",
					paymentIndex + 1,
					generatedRefundCount,
					paymentIndex + 1 + generatedRefundCount,
					paymentIndex + 1 + generatedRefundCount
				);
			}
		}

		if (!targetRows.isEmpty()) {
			insertSettlementTargetRows(targetRows);
		}
		if (!calculationRows.isEmpty()) {
			insertSettlementTargetCalculationRows(calculationRows);
		}

		insertSellerGradesAndSettlements(
			sellerIds,
			recentThreeMonthSalesCents,
			targetMonthSalesCents,
			gradePolicies,
			targetMonth,
			random
		);

		log.info(
			"[SETTLEMENT_PERF_SEED] inserted paymentTargets={} refundTargets={} totalTargets={} totalCalculations={}",
			totalPaymentTargetCount,
			generatedRefundCount,
			totalPaymentTargetCount + generatedRefundCount,
			totalPaymentTargetCount + generatedRefundCount
		);
	}

	private void insertSellerGradesAndSettlements(
		List<UUID> sellerIds,
		long[] recentThreeMonthSalesCents,
		long[] targetMonthSalesCents,
		List<GradePolicyRow> gradePolicies,
		YearMonth targetMonth,
		Random random
	) {
		LocalDateTime now = LocalDateTime.now();
		List<SellerGradeSeedRow> sellerGradeRows = new ArrayList<>(properties.getBatchSize());
		List<SettlementSeedRow> settlementRows = new ArrayList<>(properties.getBatchSize());

		for (int sellerIndex = 0; sellerIndex < sellerIds.size(); sellerIndex++) {
			UUID sellerId = sellerIds.get(sellerIndex);
			BigDecimal recentSalesAmount = amount(Math.max(recentThreeMonthSalesCents[sellerIndex], 0L));
			GradePolicyRow gradePolicy = resolveGradePolicy(recentSalesAmount, gradePolicies);

			sellerGradeRows.add(new SellerGradeSeedRow(
				deterministicUuid("seller-grade", sellerIndex),
				now,
				now,
				sellerId,
				gradePolicy.id(),
				targetMonth.toString()
			));

			if (targetMonthSalesCents[sellerIndex] == 0L || random.nextDouble() >= properties.getExistingSettlementRatio()) {
				if (sellerGradeRows.size() >= properties.getBatchSize()) {
					insertSellerGradeRows(sellerGradeRows);
					sellerGradeRows.clear();
				}
				continue;
			}

			BigDecimal originalAmount = amount(targetMonthSalesCents[sellerIndex]);
			BigDecimal feeAmount = calculateFeeAmount(originalAmount, gradePolicy.feeRate());
			BigDecimal settlementAmount = originalAmount.subtract(feeAmount).setScale(2, RoundingMode.DOWN);
			String status = pickExistingSettlementStatus(random, settlementAmount);
			LocalDateTime transferredAt = SENT.equals(status) ? now.minusDays(random.nextInt(10)) : null;
			String failReason = switch (status) {
				case HOLD -> "목데이터: 정산 금액이 0 이하이므로 송금 보류";
				case FAILED -> "목데이터: 송금 실패";
				default -> null;
			};

			settlementRows.add(new SettlementSeedRow(
				deterministicUuid("settlement", sellerIndex + ":" + targetMonth),
				now,
				now,
				sellerId,
				targetMonth.toString(),
				originalAmount,
				gradePolicy.gradeCode(),
				gradePolicy.id(),
				recentSalesAmount,
				feeAmount,
				gradePolicy.feeRate(),
				settlementAmount,
				status,
				transferredAt,
				failReason
			));

			if (sellerGradeRows.size() >= properties.getBatchSize()) {
				insertSellerGradeRows(sellerGradeRows);
				sellerGradeRows.clear();
			}
			if (settlementRows.size() >= properties.getBatchSize()) {
				insertSettlementRows(settlementRows);
				settlementRows.clear();
			}
		}

		if (!sellerGradeRows.isEmpty()) {
			insertSellerGradeRows(sellerGradeRows);
		}
		if (!settlementRows.isEmpty()) {
			insertSettlementRows(settlementRows);
		}
	}

	private String pickExistingSettlementStatus(Random random, BigDecimal settlementAmount) {
		if (settlementAmount.compareTo(ZERO) <= 0) {
			return HOLD;
		}

		double draw = random.nextDouble();
		if (draw < 0.78d) {
			return READY;
		}
		if (draw < 0.88d) {
			return FAILED;
		}
		if (draw < 0.94d) {
			return TRANSFERRING;
		}
		return SENT;
	}

	private BigDecimal calculateFeeAmount(BigDecimal originalAmount, BigDecimal feeRate) {
		return originalAmount.multiply(feeRate).setScale(2, RoundingMode.DOWN);
	}

	private GradePolicyRow resolveGradePolicy(BigDecimal salesAmount, List<GradePolicyRow> gradePolicies) {
		BigDecimal normalizedSalesAmount = salesAmount.compareTo(ZERO) < 0 ? ZERO : salesAmount;
		return gradePolicies.stream()
			.filter(policy -> policy.minSalesAmount().compareTo(normalizedSalesAmount) <= 0)
			.filter(policy -> policy.maxSalesAmount() == null || policy.maxSalesAmount().compareTo(normalizedSalesAmount) >= 0)
			.findFirst()
			.orElseThrow(() -> new IllegalStateException("판매 금액에 매칭되는 등급 정책이 없습니다. salesAmount=" + normalizedSalesAmount));
	}

	private void accumulateSales(
		long[] recentThreeMonthSalesCents,
		int sellerIndex,
		YearMonth rowMonth,
		long amountCents,
		YearMonth targetMonth,
		YearMonth previousMonth,
		YearMonth twoMonthsAgo
	) {
		if (rowMonth.equals(targetMonth) || rowMonth.equals(previousMonth) || rowMonth.equals(twoMonthsAgo)) {
			recentThreeMonthSalesCents[sellerIndex] += amountCents;
		}
	}

	private void accumulateTargetMonthSales(
		long[] targetMonthSalesCents,
		int sellerIndex,
		YearMonth rowMonth,
		long amountCents,
		YearMonth targetMonth
	) {
		if (rowMonth.equals(targetMonth)) {
			targetMonthSalesCents[sellerIndex] += amountCents;
		}
	}

	private PromotionApplication resolvePromotion(
		PromotionRow promotion,
		boolean promotedSeller,
		LocalDateTime occurredAt,
		LocalDateTime promotionStartedAt,
		LocalDateTime promotionEndedAt
	) {
		if (promotion == null || !promotedSeller) {
			return PromotionApplication.empty();
		}
		if (occurredAt.isBefore(promotionStartedAt) || occurredAt.isAfter(promotionEndedAt)) {
			return PromotionApplication.empty();
		}
		return new PromotionApplication(promotion.id(), "NEW_SELLER", promotion.feeRate());
	}

	private int pickSellerIndex(Random random, int sellerCount) {
		return (int)Math.floor(Math.pow(random.nextDouble(), 2.2d) * sellerCount);
	}

	private YearMonth resolvePaymentMonth(
		int paymentIndex,
		int targetMonthPaymentCount,
		int previousMonthPaymentCount,
		YearMonth targetMonth,
		YearMonth previousMonth,
		YearMonth twoMonthsAgo
	) {
		if (paymentIndex < targetMonthPaymentCount) {
			return targetMonth;
		}
		if (paymentIndex < targetMonthPaymentCount + previousMonthPaymentCount) {
			return previousMonth;
		}
		return twoMonthsAgo;
	}

	private LocalDateTime randomDateTimeInMonth(Random random, YearMonth yearMonth) {
		int day = 1 + random.nextInt(yearMonth.lengthOfMonth());
		int hour = random.nextInt(24);
		int minute = random.nextInt(60);
		int second = random.nextInt(60);
		return yearMonth.atDay(day).atTime(hour, minute, second);
	}

	private long generatePaymentAmountCents(Random random) {
		double draw = random.nextDouble();
		if (draw < 0.70d) {
			return 10_000L + random.nextInt(90_001);
		}
		if (draw < 0.95d) {
			return 100_000L + random.nextInt(400_001);
		}
		return 500_000L + random.nextInt(1_500_001);
	}

	private long generateRefundAmountCents(Random random, long paymentAmountCents) {
		if (paymentAmountCents <= 1L) {
			return 1L;
		}
		double draw = random.nextDouble();
		if (draw < 0.70d) {
			return Math.max(1L, (long)(paymentAmountCents * (0.10d + random.nextDouble() * 0.40d)));
		}
		if (draw < 0.90d) {
			return Math.max(1L, (long)(paymentAmountCents * (0.50d + random.nextDouble() * 0.40d)));
		}
		return paymentAmountCents;
	}

	private UUID deterministicUuid(String namespace, Object key) {
		String source = "settlement-perf:" + namespace + ":" + key;
		return UUID.nameUUIDFromBytes(source.getBytes(StandardCharsets.UTF_8));
	}

	private BigDecimal amount(long amountCents) {
		return BigDecimal.valueOf(amountCents).divide(ONE_HUNDRED, 2, RoundingMode.DOWN);
	}

	private void insertSettlementTargetRows(List<SettlementTargetSeedRow> rows) {
		jdbcTemplate.batchUpdate(
			"""
				INSERT INTO settlement_targets (
					id,
					created_at,
					updated_at,
					source_event_id,
					settlement_month,
					seller_id,
					order_id,
					payment_id,
					refund_id,
					product_id,
					target_type,
					settlement_base_amount,
					occurred_at,
					calculation_status,
					calculation_requested_at,
					calculation_completed_at,
					calculation_failed_reason
				) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
				""",
			new BatchPreparedStatementSetter() {
				@Override
				public void setValues(PreparedStatement ps, int i) throws java.sql.SQLException {
					SettlementTargetSeedRow row = rows.get(i);
					ps.setObject(1, row.id());
					ps.setTimestamp(2, Timestamp.valueOf(row.createdAt()));
					ps.setTimestamp(3, Timestamp.valueOf(row.updatedAt()));
					ps.setObject(4, row.sourceEventId());
					ps.setString(5, row.settlementMonth());
					ps.setObject(6, row.sellerId());
					ps.setObject(7, row.orderId());
					ps.setObject(8, row.paymentId());
					ps.setObject(9, row.refundId());
					ps.setObject(10, row.productId());
					ps.setString(11, row.targetType());
					ps.setBigDecimal(12, row.settlementBaseAmount());
					ps.setTimestamp(13, Timestamp.valueOf(row.occurredAt()));
					ps.setString(14, row.calculationStatus());
					ps.setTimestamp(15, Timestamp.valueOf(row.calculationRequestedAt()));
					ps.setTimestamp(16, Timestamp.valueOf(row.calculationCompletedAt()));
					ps.setString(17, row.calculationFailedReason());
				}

				@Override
				public int getBatchSize() {
					return rows.size();
				}
			}
		);
	}

	private void insertSettlementTargetCalculationRows(List<SettlementTargetCalculationSeedRow> rows) {
		jdbcTemplate.batchUpdate(
			"""
				INSERT INTO settlement_target_calculations (
					id,
					created_at,
					updated_at,
					settlement_target_id,
					settlement_month,
					seller_id,
					settlement_base_amount,
					applied_promotion_id,
					applied_promotion_type,
					applied_fee_rate,
					original_payment_target_calculation_id,
					calculated_at
				) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
				""",
			new BatchPreparedStatementSetter() {
				@Override
				public void setValues(PreparedStatement ps, int i) throws java.sql.SQLException {
					SettlementTargetCalculationSeedRow row = rows.get(i);
					ps.setObject(1, row.id());
					ps.setTimestamp(2, Timestamp.valueOf(row.createdAt()));
					ps.setTimestamp(3, Timestamp.valueOf(row.updatedAt()));
					ps.setObject(4, row.settlementTargetId());
					ps.setString(5, row.settlementMonth());
					ps.setObject(6, row.sellerId());
					ps.setBigDecimal(7, row.settlementBaseAmount());
					ps.setObject(8, row.appliedPromotionId());
					ps.setString(9, row.appliedPromotionType());
					ps.setBigDecimal(10, row.appliedFeeRate());
					ps.setObject(11, row.originalPaymentTargetCalculationId());
					ps.setTimestamp(12, Timestamp.valueOf(row.calculatedAt()));
				}

				@Override
				public int getBatchSize() {
					return rows.size();
				}
			}
		);
	}

	private void insertSellerPromotionRows(List<SellerPromotionSeedRow> rows, int batchSize) {
		for (int start = 0; start < rows.size(); start += batchSize) {
			List<SellerPromotionSeedRow> chunk = rows.subList(start, Math.min(start + batchSize, rows.size()));
			jdbcTemplate.batchUpdate(
				"""
					INSERT INTO seller_promotions (
						id,
						created_at,
						updated_at,
						seller_id,
						promotion_id,
						started_at,
						ended_at,
						active
					) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
					""",
				new BatchPreparedStatementSetter() {
					@Override
					public void setValues(PreparedStatement ps, int i) throws java.sql.SQLException {
						SellerPromotionSeedRow row = chunk.get(i);
						ps.setObject(1, row.id());
						ps.setTimestamp(2, Timestamp.valueOf(row.createdAt()));
						ps.setTimestamp(3, Timestamp.valueOf(row.updatedAt()));
						ps.setObject(4, row.sellerId());
						ps.setObject(5, row.promotionId());
						ps.setTimestamp(6, Timestamp.valueOf(row.startedAt()));
						ps.setTimestamp(7, Timestamp.valueOf(row.endedAt()));
						ps.setBoolean(8, row.active());
					}

					@Override
					public int getBatchSize() {
						return chunk.size();
					}
				}
			);
		}
	}

	private void insertSellerGradeRows(List<SellerGradeSeedRow> rows) {
		jdbcTemplate.batchUpdate(
			"""
				INSERT INTO seller_grades (
					id,
					created_at,
					updated_at,
					seller_id,
					seller_grade_policy_id,
					calculated_month
				) VALUES (?, ?, ?, ?, ?, ?)
				""",
			new BatchPreparedStatementSetter() {
				@Override
				public void setValues(PreparedStatement ps, int i) throws java.sql.SQLException {
					SellerGradeSeedRow row = rows.get(i);
					ps.setObject(1, row.id());
					ps.setTimestamp(2, Timestamp.valueOf(row.createdAt()));
					ps.setTimestamp(3, Timestamp.valueOf(row.updatedAt()));
					ps.setObject(4, row.sellerId());
					ps.setObject(5, row.sellerGradePolicyId());
					ps.setString(6, row.calculatedMonth());
				}

				@Override
				public int getBatchSize() {
					return rows.size();
				}
			}
		);
	}

	private void insertSettlementRows(List<SettlementSeedRow> rows) {
		jdbcTemplate.batchUpdate(
			"""
				INSERT INTO settlements (
					id,
					created_at,
					updated_at,
					seller_id,
					settlement_month,
					original_amount,
					seller_grade_code,
					seller_grade_policy_id,
					grade_base_amount,
					fee_amount,
					fee_rate,
					settlement_amount,
					status,
					transferred_at,
					fail_reason
				) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
				""",
			new BatchPreparedStatementSetter() {
				@Override
				public void setValues(PreparedStatement ps, int i) throws java.sql.SQLException {
					SettlementSeedRow row = rows.get(i);
					ps.setObject(1, row.id());
					ps.setTimestamp(2, Timestamp.valueOf(row.createdAt()));
					ps.setTimestamp(3, Timestamp.valueOf(row.updatedAt()));
					ps.setObject(4, row.sellerId());
					ps.setString(5, row.settlementMonth());
					ps.setBigDecimal(6, row.originalAmount());
					ps.setString(7, row.sellerGradeCode());
					ps.setObject(8, row.sellerGradePolicyId());
					ps.setBigDecimal(9, row.gradeBaseAmount());
					ps.setBigDecimal(10, row.feeAmount());
					ps.setBigDecimal(11, row.feeRate());
					ps.setBigDecimal(12, row.settlementAmount());
					ps.setString(13, row.status());
					ps.setTimestamp(14, row.transferredAt() == null ? null : Timestamp.valueOf(row.transferredAt()));
					ps.setString(15, row.failReason());
				}

				@Override
				public int getBatchSize() {
					return rows.size();
				}
			}
		);
	}

	private record GradePolicyRow(
		UUID id,
		String gradeCode,
		BigDecimal minSalesAmount,
		BigDecimal maxSalesAmount,
		BigDecimal feeRate
	) {
	}

	private record PromotionRow(
		UUID id,
		BigDecimal feeRate,
		int durationDays
	) {
	}

	private record PromotionApplication(
		UUID promotionId,
		String promotionType,
		BigDecimal feeRate
	) {
		private static PromotionApplication empty() {
			return new PromotionApplication(null, null, null);
		}
	}

	private record SellerPromotionSeedRow(
		UUID id,
		LocalDateTime createdAt,
		LocalDateTime updatedAt,
		UUID sellerId,
		UUID promotionId,
		LocalDateTime startedAt,
		LocalDateTime endedAt,
		boolean active
	) {
	}

	private record SettlementTargetSeedRow(
		UUID id,
		LocalDateTime createdAt,
		LocalDateTime updatedAt,
		UUID sourceEventId,
		String settlementMonth,
		UUID sellerId,
		UUID orderId,
		UUID paymentId,
		UUID refundId,
		UUID productId,
		String targetType,
		BigDecimal settlementBaseAmount,
		LocalDateTime occurredAt,
		String calculationStatus,
		LocalDateTime calculationRequestedAt,
		LocalDateTime calculationCompletedAt,
		String calculationFailedReason
	) {
	}

	private record SettlementTargetCalculationSeedRow(
		UUID id,
		LocalDateTime createdAt,
		LocalDateTime updatedAt,
		UUID settlementTargetId,
		String settlementMonth,
		UUID sellerId,
		BigDecimal settlementBaseAmount,
		UUID appliedPromotionId,
		String appliedPromotionType,
		BigDecimal appliedFeeRate,
		UUID originalPaymentTargetCalculationId,
		LocalDateTime calculatedAt
	) {
	}

	private record SellerGradeSeedRow(
		UUID id,
		LocalDateTime createdAt,
		LocalDateTime updatedAt,
		UUID sellerId,
		UUID sellerGradePolicyId,
		String calculatedMonth
	) {
	}

	private record SettlementSeedRow(
		UUID id,
		LocalDateTime createdAt,
		LocalDateTime updatedAt,
		UUID sellerId,
		String settlementMonth,
		BigDecimal originalAmount,
		String sellerGradeCode,
		UUID sellerGradePolicyId,
		BigDecimal gradeBaseAmount,
		BigDecimal feeAmount,
		BigDecimal feeRate,
		BigDecimal settlementAmount,
		String status,
		LocalDateTime transferredAt,
		String failReason
	) {
	}
}
