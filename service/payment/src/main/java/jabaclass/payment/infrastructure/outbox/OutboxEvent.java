package jabaclass.payment.infrastructure.outbox;

import java.util.UUID;

import jabaclass.payment.domain.model.BaseEntity;
import jakarta.persistence.Entity;
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

	private boolean published;

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
		event.published = false;
		return event;
	}

	public void markPublished() {
		this.published = true;
	}
}