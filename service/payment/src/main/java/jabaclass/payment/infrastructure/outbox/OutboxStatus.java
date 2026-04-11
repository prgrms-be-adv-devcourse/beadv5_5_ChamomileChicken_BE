package jabaclass.payment.infrastructure.outbox;

public enum OutboxStatus {
	PENDING,
	PUBLISHED,
	FAILED   // DLQ 상태
}