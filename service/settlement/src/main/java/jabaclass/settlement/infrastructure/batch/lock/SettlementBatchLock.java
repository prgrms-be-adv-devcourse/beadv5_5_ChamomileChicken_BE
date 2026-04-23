package jabaclass.settlement.infrastructure.batch.lock;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import jabaclass.settlement.domain.model.BaseEntity;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
	name = "settlement_batch_locks",
	uniqueConstraints = @UniqueConstraint(
		name = "uk_settlement_batch_locks_lock_key",
		columnNames = "lock_key"
	)
)
public class SettlementBatchLock extends BaseEntity {

	@Column(name = "lock_key", nullable = false, length = 100)
	private String lockKey;

	@Column(name = "job_name", nullable = false, length = 100)
	private String jobName;

	@Column(name = "settlement_month", nullable = false, length = 7)
	private String settlementMonth;

	@Column(name = "expires_at", nullable = false)
	private LocalDateTime expiresAt;

	public SettlementBatchLock(
		String lockKey,
		String jobName,
		String settlementMonth,
		LocalDateTime expiresAt
	) {
		this.lockKey = lockKey;
		this.jobName = jobName;
		this.settlementMonth = settlementMonth;
		this.expiresAt = expiresAt;
	}
}
