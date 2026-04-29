package jabaclass.settlement.infrastructure.batch.listener;

import java.sql.Timestamp;
import java.time.LocalDateTime;

import org.springframework.batch.core.listener.SkipListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import jabaclass.settlement.infrastructure.batch.dto.RefundTargetCalculationItem;
import lombok.RequiredArgsConstructor;
@Component
@RequiredArgsConstructor
public class SettlementRefundSkipListener
	implements SkipListener<RefundTargetCalculationItem, Object> {

	private final JdbcTemplate jdbcTemplate;

	@Override
	public void onSkipInProcess(RefundTargetCalculationItem item, Throwable t) {
		LocalDateTime now = LocalDateTime.now();
		jdbcTemplate.update(
			"""
				UPDATE settlement_targets
				SET updated_at = ?,
				    calculation_status = ?,
				    calculation_completed_at = ?,
				    calculation_failed_reason = ?
				WHERE id = ?
				""",
			Timestamp.valueOf(now),
			"FAILED",
			Timestamp.valueOf(now),
			truncateReason(t == null ? "calculation skipped" : t.getMessage()),
			item.target().getId()
		);
	}

	private String truncateReason(String reason) {
		if (reason == null) {
			return "unknown";
		}
		return reason.length() > 500 ? reason.substring(0, 500) : reason;
	}
}
