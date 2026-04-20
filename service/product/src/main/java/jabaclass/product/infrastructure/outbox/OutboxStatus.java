package jabaclass.product.infrastructure.outbox;

public enum OutboxStatus {
	PENDING,
	SENDING,
	PUBLISHED,
	FAILED
}
