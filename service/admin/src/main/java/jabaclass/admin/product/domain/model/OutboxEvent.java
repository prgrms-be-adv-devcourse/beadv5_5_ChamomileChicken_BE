package jabaclass.admin.product.domain.model;

import java.time.LocalDateTime;

import jabaclass.admin.common.model.BaseEntity;
import jabaclass.admin.product.infrastructure.outbox.EventType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.Getter;

@Getter
@Entity
@Table(name = "admin_outbox_events")
public class OutboxEvent extends BaseEntity {

	@Column(nullable = false)
	private String aggregateType;

	@Column(nullable = false)
	private String aggregateId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private EventType eventType;

	@Lob
	@Column(nullable = false)
	private String payload;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private OutboxStatus status;

	@Column(nullable = false)
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
		event.aggregateType = aggregateType;
		event.aggregateId = aggregateId;
		event.eventType = eventType;
		event.payload = payload;
		event.status = OutboxStatus.PENDING;
		event.retryCount = 0;
		return event;
	}

	public void markSending() {
		this.status = OutboxStatus.SENDING;
		this.lastAttemptAt = LocalDateTime.now();
	}

	public void markPending() {
		this.status = OutboxStatus.PENDING;
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