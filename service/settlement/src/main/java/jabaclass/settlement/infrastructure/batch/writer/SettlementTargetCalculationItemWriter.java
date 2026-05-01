package jabaclass.settlement.infrastructure.batch.writer;

import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.batch.infrastructure.item.Chunk;
import org.springframework.batch.infrastructure.item.ItemWriter;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import jabaclass.settlement.domain.model.settlement.SettlementTargetCalculation;
import jabaclass.settlement.domain.model.settlement.SettlementTargetCalculationStatus;
import jabaclass.settlement.infrastructure.batch.dto.SettlementTargetCalculationBatchItem;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class SettlementTargetCalculationItemWriter implements ItemWriter<SettlementTargetCalculationBatchItem> {

	private static final int FAILED_REASON_MAX_LENGTH = 500;

	private final JdbcTemplate jdbcTemplate;
	private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

	@Override
	public void write(Chunk<? extends SettlementTargetCalculationBatchItem> items) {
		List<SettlementTargetCalculation> calculations = items.getItems().stream()
			.map(SettlementTargetCalculationBatchItem::calculation)
			.filter(Objects::nonNull)
			.toList();

		if (!calculations.isEmpty()) {
			insertCalculations(calculations);
		}
		if (!items.getItems().isEmpty()) {
			updateTargets(items.getItems());
		}
	}

	private void insertCalculations(List<SettlementTargetCalculation> calculations) {
		LocalDateTime now = LocalDateTime.now();
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
					SettlementTargetCalculation calculation = calculations.get(i);
					ps.setObject(1, UUID.randomUUID());
					ps.setTimestamp(2, Timestamp.valueOf(now));
					ps.setTimestamp(3, Timestamp.valueOf(now));
					ps.setObject(4, calculation.getSettlementTargetId());
					ps.setString(5, calculation.getSettlementMonth());
					ps.setObject(6, calculation.getSellerId());
					ps.setBigDecimal(7, calculation.getSettlementBaseAmount());
					ps.setObject(8, calculation.getAppliedPromotionId());
					ps.setString(9, calculation.getAppliedPromotionType());
					ps.setBigDecimal(10, calculation.getAppliedFeeRate());
					ps.setObject(11, calculation.getOriginalPaymentTargetCalculationId());
					ps.setTimestamp(12, Timestamp.valueOf(calculation.getCalculatedAt()));
				}

				@Override
				public int getBatchSize() {
					return calculations.size();
				}
			}
		);
	}

	private void updateTargets(List<? extends SettlementTargetCalculationBatchItem> items) {
		List<? extends SettlementTargetCalculationBatchItem> calculatedTargets = items.stream()
			.filter(item -> item.calculationStatus() == SettlementTargetCalculationStatus.CALCULATED)
			.toList();
		List<? extends SettlementTargetCalculationBatchItem> nonCalculatedTargets = items.stream()
			.filter(item -> item.calculationStatus() != SettlementTargetCalculationStatus.CALCULATED)
			.toList();

		if (!calculatedTargets.isEmpty()) {
			updateCalculatedTargets(calculatedTargets);
		}
		if (!nonCalculatedTargets.isEmpty()) {
			updateTargetsIndividually(nonCalculatedTargets);
		}
	}

	private void updateCalculatedTargets(List<? extends SettlementTargetCalculationBatchItem> items) {
		LocalDateTime now = LocalDateTime.now();
		namedParameterJdbcTemplate.update(
			"""
				UPDATE settlement_targets
				SET updated_at = :updatedAt,
				    calculation_status = :calculationStatus,
				    calculation_completed_at = :calculationCompletedAt,
				    calculation_failed_reason = NULL
				WHERE id IN (:ids)
				""",
			new MapSqlParameterSource()
				.addValue("updatedAt", Timestamp.valueOf(now))
				.addValue("calculationStatus", SettlementTargetCalculationStatus.CALCULATED.name())
				.addValue("calculationCompletedAt", Timestamp.valueOf(now))
				.addValue("ids", items.stream().map(item -> item.target().id()).collect(Collectors.toList()))
		);
	}

	private void updateTargetsIndividually(List<? extends SettlementTargetCalculationBatchItem> items) {
		jdbcTemplate.batchUpdate(
			"""
				UPDATE settlement_targets
				SET updated_at = ?,
				    calculation_status = ?,
				    calculation_completed_at = ?,
				    calculation_failed_reason = ?
				WHERE id = ?
				""",
			new BatchPreparedStatementSetter() {
				@Override
				public void setValues(PreparedStatement ps, int i) throws java.sql.SQLException {
					SettlementTargetCalculationBatchItem item = items.get(i);
					ps.setTimestamp(1, Timestamp.valueOf(item.calculationCompletedAt()));
					ps.setString(2, item.calculationStatus().name());
					ps.setTimestamp(3, item.calculationCompletedAt() == null ? null : Timestamp.valueOf(item.calculationCompletedAt()));
					ps.setString(4, truncateReason(item.calculationFailedReason()));
					ps.setObject(5, item.target().id());
				}

				@Override
				public int getBatchSize() {
					return items.size();
				}
			}
		);
	}

	private String truncateReason(String reason) {
		if (reason == null) {
			return null;
		}
		return reason.length() > FAILED_REASON_MAX_LENGTH
			? reason.substring(0, FAILED_REASON_MAX_LENGTH)
			: reason;
	}

}
