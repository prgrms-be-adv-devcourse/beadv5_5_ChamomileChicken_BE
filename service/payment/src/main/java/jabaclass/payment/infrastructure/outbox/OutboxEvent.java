package jabaclass.payment.infrastructure.outbox;

import java.time.LocalDateTime;
import java.util.UUID;

import jabaclass.payment.domain.model.BaseEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.Getter;

@Entity
@Table(name = "outbox_events")
@Getter
public class OutboxEvent extends BaseEntity {

	@Id
	private UUID id;

	private String aggregateType;
	private String aggregateId;
	private EventType eventType;

	@Lob
	private String payload;

	@Enumerated(EnumType.STRING)
	private OutboxStatus status;

	private int retryCount;
	private LocalDateTime lastAttemptAt;

	protected OutboxEvent() {}

	public static OutboxEvent create(
		String aggregateType,
		String aggregateId,
		EventType eventType,
		String payload
	) {
		OutboxEvent event = new OutboxEvent();
		event.id = UUID.randomUUID();
		event.aggregateType = aggregateType;
		event.aggregateId = aggregateId;
		event.eventType = eventType;
		event.payload = payload;
		event.status = OutboxStatus.PENDING;
		event.retryCount = 0;
		return event;
	}

	public void markPublished() {
		this.status = OutboxStatus.PUBLISHED;
	}

	public void markFailed() {
		this.status = OutboxStatus.FAILED;
	}

	public void increaseRetry() {
		this.retryCount++;
		this.lastAttemptAt = LocalDateTime.now();
	}

	public boolean isRetryExceeded() {
		return this.retryCount >= 5;
	}
}