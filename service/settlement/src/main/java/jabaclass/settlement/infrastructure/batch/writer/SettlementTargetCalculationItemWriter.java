package jabaclass.settlement.infrastructure.batch.writer;

import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.springframework.batch.infrastructure.item.Chunk;
import org.springframework.batch.infrastructure.item.ItemWriter;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import jabaclass.settlement.domain.model.settlement.SettlementTarget;
import jabaclass.settlement.domain.model.settlement.SettlementTargetCalculation;
import jabaclass.settlement.infrastructure.batch.dto.SettlementTargetCalculationBatchItem;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class SettlementTargetCalculationItemWriter implements ItemWriter<SettlementTargetCalculationBatchItem> {

	private final JdbcTemplate jdbcTemplate;

	@Override
	public void write(Chunk<? extends SettlementTargetCalculationBatchItem> items) {
		List<SettlementTarget> targets = items.getItems().stream()
			.map(SettlementTargetCalculationBatchItem::target)
			.toList();
		List<SettlementTargetCalculation> calculations = items.getItems().stream()
			.map(SettlementTargetCalculationBatchItem::calculation)
			.filter(Objects::nonNull)
			.toList();

		if (!calculations.isEmpty()) {
			insertCalculations(calculations);
		}
		if (!targets.isEmpty()) {
			updateTargets(targets);
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

	private void updateTargets(List<SettlementTarget> targets) {
		LocalDateTime now = LocalDateTime.now();
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
					SettlementTarget target = targets.get(i);
					ps.setTimestamp(1, Timestamp.valueOf(now));
					ps.setString(2, target.getCalculationStatus().name());
					ps.setTimestamp(3, target.getCalculationCompletedAt() == null ? null : Timestamp.valueOf(target.getCalculationCompletedAt()));
					ps.setString(4, target.getCalculationFailedReason());
					ps.setObject(5, target.getId());
				}

				@Override
				public int getBatchSize() {
					return targets.size();
				}
			}
		);
	}
}
