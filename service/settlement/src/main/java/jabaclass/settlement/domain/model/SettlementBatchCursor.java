package jabaclass.settlement.domain.model;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "settlement_batch_cursor")
public class SettlementBatchCursor extends BaseEntity {

	/**
	 * PAYMENT / REFUND
	 */
	@Column(name = "cursor_type", nullable = false, length = 30, unique = true)
	private String cursorType;

	@Column(name = "last_synced_at", nullable = false)
	private LocalDateTime lastSyncedAt;

	@Column(name = "last_synced_id")
	private UUID lastSyncedId;

	public SettlementBatchCursor(
		String cursorType,
		LocalDateTime lastSyncedAt,
		UUID lastSyncedId
	) {
		validateCursorType(cursorType);
		validateLastSyncedAt(lastSyncedAt);

		this.cursorType = cursorType;
		this.lastSyncedAt = lastSyncedAt;
		this.lastSyncedId = lastSyncedId;
	}

	public static SettlementBatchCursor initial(String cursorType, LocalDateTime lastSyncedAt) {
		return new SettlementBatchCursor(
			cursorType,
			lastSyncedAt,
			null
		);
	}

	public void advance(LocalDateTime lastSyncedAt, UUID lastSyncedId) {
		validateLastSyncedAt(lastSyncedAt);

		this.lastSyncedAt = lastSyncedAt;
		this.lastSyncedId = lastSyncedId;
	}

	private void validateCursorType(String cursorType) {
		if (cursorType == null || cursorType.isBlank()) {
			throw new IllegalArgumentException("커서 타입은 비어 있을 수 없습니다.");
		}
	}

	private void validateLastSyncedAt(LocalDateTime lastSyncedAt) {
		if (lastSyncedAt == null) {
			throw new IllegalArgumentException("마지막 동기화 시각은 null일 수 없습니다.");
		}
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof SettlementBatchCursor that)) return false;
		return Objects.equals(getId(), that.getId());
	}

	@Override
	public int hashCode() {
		return Objects.hash(getId());
	}
}
