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

import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@Profile("dev-local")
@RequiredArgsConstructor
public class SettlementPerfDataSeeder {

	private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");
	private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2, RoundingMode.DOWN);
	private static final String PAYMENT = "PAYMENT";
	private static final String REFUND = "REFUND";
	private static final String PENDING = "PENDING";
	private static final String CALCULATED = "CALCULATED";

	private final JdbcTemplate jdbcTemplate;
	private final SettlementPerfSeedProperties properties;

	@Transactional
	public void seed() {
		if (!properties.isEnabled()) {
			throw new IllegalStateException("settlement.perf.seed.enabled=true 일 때만 성능 시드를 실행할 수 있습니다.");
		}

		validateProperties();

		if (properties.isTruncateBeforeSeed()) {
			truncateSeedTables();
		}

		PromotionRow promotion = loadNewSellerPromotion();

		log.info(
			"[SETTLEMENT_PERF_SEED] start sellerCount={} targetMonthPaymentCount={} previousMonthPaymentCount={} twoMonthsAgoPaymentCount={} refundRatio={} promotedSellerRatio={} targetMonth={} batchSize={}",
			properties.getSellerCount(),
			properties.getTargetMonthPaymentCount(),
			properties.getPreviousMonthPaymentCount(),
			properties.getTwoMonthsAgoPaymentCount(),
			properties.getRefundRatio(),
			properties.getPromotedSellerRatio(),
			properties.getTargetMonth(),
			properties.getBatchSize()
		);

		seedPerfData(promotion);

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

	private void seedPerfData(PromotionRow promotion) {
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

		for (int sellerIndex = 0; sellerIndex < properties.getSellerCount(); sellerIndex++) {
			sellerIds.add(deterministicUuid("seller", sellerIndex));
			promotedSellerFlags[sellerIndex] = promotion != null
				&& random.nextDouble() < properties.getPromotedSellerRatio();
		}

		upsertSellerUsers(sellerIds, now);
		upsertSellerSettlementAccounts(sellerIds, now);
		log.info("[SETTLEMENT_PERF_SEED] upserted users/seller_settlement_accounts count={}", sellerIds.size());

		LocalDateTime promotionStartedAt = targetMonth.atDay(1).atStartOfDay();
		LocalDateTime promotionEndedAt = promotion == null
			? null
			: promotionStartedAt.plusDays(promotion.durationDays()).minusNanos(1);

		if (promotion != null) {
			List<SellerPromotionSeedRow> sellerPromotionRows = new ArrayList<>();
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
		long preCalculatedCount = 0L;

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
			PromotionApplication paymentPromotion = resolvePromotion(
				promotion,
				promotedSellerFlags[sellerIndex],
				paymentOccurredAt,
				promotionStartedAt,
				promotionEndedAt
			);
			boolean preCalculatedPayment = !paymentMonth.equals(targetMonth);
			LocalDateTime paymentRequestedAt = preCalculatedPayment ? paymentOccurredAt.plusSeconds(10) : null;
			LocalDateTime paymentCompletedAt = preCalculatedPayment ? paymentOccurredAt.plusSeconds(30) : null;

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
				preCalculatedPayment ? CALCULATED : PENDING,
				paymentRequestedAt,
				paymentCompletedAt,
				null
			));

			if (preCalculatedPayment) {
				preCalculatedCount++;
				calculationRows.add(new SettlementTargetCalculationSeedRow(
					paymentCalculationId,
					now,
					now,
					paymentTargetId,
					paymentMonth.toString(),
					sellerId,
					paymentAmount,
					paymentPromotion.promotionId(),
					paymentPromotion.promotionType(),
					paymentPromotion.feeRate(),
					null,
					paymentCompletedAt
				));
			}

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
				boolean preCalculatedRefund = !refundMonth.equals(targetMonth);
				LocalDateTime refundRequestedAt = preCalculatedRefund ? refundOccurredAt.plusSeconds(10) : null;
				LocalDateTime refundCompletedAt = preCalculatedRefund ? refundOccurredAt.plusSeconds(30) : null;

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
					preCalculatedRefund ? CALCULATED : PENDING,
					refundRequestedAt,
					refundCompletedAt,
					null
				));

				if (preCalculatedRefund && preCalculatedPayment) {
					preCalculatedCount++;
					calculationRows.add(new SettlementTargetCalculationSeedRow(
						refundCalculationId,
						now,
						now,
						refundTargetId,
						refundMonth.toString(),
						sellerId,
						calculateRefundSettlementBaseAmount(paymentAmount, refundAmount),
						paymentPromotion.promotionId(),
						paymentPromotion.promotionType(),
						paymentPromotion.feeRate(),
						paymentCalculationId,
						refundCompletedAt
					));
				}
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
					"[SETTLEMENT_PERF_SEED] progress paymentTargets={} refundTargets={} targetRowsInsertedSoFar={} preCalculatedRows={}",
					paymentIndex + 1,
					generatedRefundCount,
					paymentIndex + 1 + generatedRefundCount,
					preCalculatedCount
				);
			}
		}

		if (!targetRows.isEmpty()) {
			insertSettlementTargetRows(targetRows);
		}
		if (!calculationRows.isEmpty()) {
			insertSettlementTargetCalculationRows(calculationRows);
		}

		log.info(
			"[SETTLEMENT_PERF_SEED] inserted paymentTargets={} refundTargets={} totalTargets={} preCalculatedRows={}",
			totalPaymentTargetCount,
			generatedRefundCount,
			totalPaymentTargetCount + generatedRefundCount,
			preCalculatedCount
		);

		validateHistoricalRefundDependencies(targetMonth.toString());
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
		if (draw < 0.55d) {
			return 3_000_000L + random.nextInt(17_000_001);
		}
		if (draw < 0.90d) {
			return 20_000_000L + random.nextInt(80_000_001);
		}
		return 100_000_000L + random.nextInt(400_000_001);
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

	private BigDecimal calculateRefundSettlementBaseAmount(BigDecimal paymentAmount, BigDecimal refundAmount) {
		BigDecimal refundRatio = refundAmount.abs().divide(paymentAmount, 8, RoundingMode.HALF_UP);
		return paymentAmount.multiply(refundRatio).setScale(2, RoundingMode.DOWN).negate();
	}

	private void validateHistoricalRefundDependencies(String targetMonth) {
		Integer missingHistoricalPaymentCalculationCount = jdbcTemplate.queryForObject(
			"""
				SELECT COUNT(*)
				FROM settlement_targets rt
				JOIN settlement_targets pt
				  ON pt.payment_id = rt.payment_id
				 AND pt.target_type = 'PAYMENT'
				LEFT JOIN settlement_target_calculations stc
				  ON stc.settlement_target_id = pt.id
				WHERE rt.settlement_month = ?
				  AND rt.target_type = 'REFUND'
				  AND pt.settlement_month <> ?
				  AND stc.id IS NULL
				""",
			Integer.class,
			targetMonth,
			targetMonth
		);

		if (missingHistoricalPaymentCalculationCount != null && missingHistoricalPaymentCalculationCount > 0) {
			throw new IllegalStateException(
				"시드 검증 실패: targetMonth refund가 참조하는 이전 달 원결제 calculation 누락 count="
					+ missingHistoricalPaymentCalculationCount
			);
		}
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
					ps.setTimestamp(15, row.calculationRequestedAt() == null ? null : Timestamp.valueOf(row.calculationRequestedAt()));
					ps.setTimestamp(16, row.calculationCompletedAt() == null ? null : Timestamp.valueOf(row.calculationCompletedAt()));
					ps.setString(17, row.calculationFailedReason());
				}

				@Override
				public int getBatchSize() {
					return rows.size();
				}
			}
		);
	}

	private void upsertSellerUsers(List<UUID> sellerIds, LocalDateTime now) {
		jdbcTemplate.batchUpdate(
			"""
				INSERT INTO users (
					id,
					created_at,
					updated_at,
					name,
					email,
					password,
					phone,
					role,
					social_type,
					social_id,
					version,
					deposit,
					refresh_token,
					last_login_ip,
					last_login_user_agent
				) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
				ON CONFLICT (id) DO UPDATE SET
					updated_at = EXCLUDED.updated_at,
					name = EXCLUDED.name,
					email = EXCLUDED.email,
					role = EXCLUDED.role,
					deposit = EXCLUDED.deposit,
					last_login_ip = EXCLUDED.last_login_ip,
					last_login_user_agent = EXCLUDED.last_login_user_agent
				""",
			new BatchPreparedStatementSetter() {
				@Override
				public void setValues(PreparedStatement ps, int i) throws java.sql.SQLException {
					UUID sellerId = sellerIds.get(i);
					ps.setObject(1, sellerId);
					ps.setTimestamp(2, Timestamp.valueOf(now));
					ps.setTimestamp(3, Timestamp.valueOf(now));
					ps.setString(4, "정산성능테스트셀러-" + i);
					ps.setString(5, "settlement-perf-seller-" + i + "@example.com");
					ps.setString(6, null);
					ps.setString(7, "010-" + String.format("%04d-%04d", i / 10_000, i % 10_000));
					ps.setString(8, "SELLER");
					ps.setString(9, null);
					ps.setString(10, null);
					ps.setLong(11, 0L);
					ps.setBigDecimal(12, ZERO);
					ps.setString(13, null);
					ps.setString(14, "127.0.0.1");
					ps.setString(15, "settlement-perf-seeder");
				}

				@Override
				public int getBatchSize() {
					return sellerIds.size();
				}
			}
		);
	}

	private void upsertSellerSettlementAccounts(List<UUID> sellerIds, LocalDateTime now) {
		jdbcTemplate.batchUpdate(
			"""
				INSERT INTO seller_settlement_accounts (
					id,
					created_at,
					updated_at,
					user_id,
					bank_code,
					account_number,
					account_holder,
					active
				) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
				ON CONFLICT (user_id) DO UPDATE SET
					updated_at = EXCLUDED.updated_at,
					bank_code = EXCLUDED.bank_code,
					account_number = EXCLUDED.account_number,
					account_holder = EXCLUDED.account_holder,
					active = EXCLUDED.active
				""",
			new BatchPreparedStatementSetter() {
				@Override
				public void setValues(PreparedStatement ps, int i) throws java.sql.SQLException {
					UUID sellerId = sellerIds.get(i);
					ps.setObject(1, deterministicUuid("seller-settlement-account", i));
					ps.setTimestamp(2, Timestamp.valueOf(now));
					ps.setTimestamp(3, Timestamp.valueOf(now));
					ps.setObject(4, sellerId);
					ps.setString(5, pickBankCode(i));
					ps.setString(6, "100-" + String.format("%06d-%06d", i / 1_000_000, i % 1_000_000));
					ps.setString(7, "정산성능테스트셀러-" + i);
					ps.setBoolean(8, true);
				}

				@Override
				public int getBatchSize() {
					return sellerIds.size();
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
					ps.setTimestamp(12, row.calculatedAt() == null ? null : Timestamp.valueOf(row.calculatedAt()));
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

	private String pickBankCode(int sellerIndex) {
		return switch (sellerIndex % 5) {
			case 0 -> "004";
			case 1 -> "088";
			case 2 -> "020";
			case 3 -> "081";
			default -> "011";
		};
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
}
