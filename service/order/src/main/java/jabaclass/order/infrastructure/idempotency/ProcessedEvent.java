package jabaclass.order.infrastructure.idempotency;

import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "order_processed_events")
public class ProcessedEvent {

	@Id
	@Column(name = "event_id", nullable = false, updatable = false)
	private UUID eventId;

	@Column(name = "processed_at", nullable = false)
	private LocalDateTime processedAt;

	protected ProcessedEvent() {
	}

	public static ProcessedEvent of(UUID eventId) {
		ProcessedEvent e = new ProcessedEvent();
		e.eventId = eventId;
		e.processedAt = LocalDateTime.now();
		return e;
	}
}
