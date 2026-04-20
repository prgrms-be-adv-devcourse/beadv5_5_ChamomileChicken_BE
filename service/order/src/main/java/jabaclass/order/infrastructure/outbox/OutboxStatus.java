package jabaclass.order.infrastructure.outbox;

public enum OutboxStatus {
	PENDING,
	SENDING,
	PUBLISHED,
	FAILED
}
