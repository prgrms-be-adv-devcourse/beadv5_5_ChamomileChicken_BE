package jabaclass.payment.infrastructure.outbox;

public enum OutboxStatus {
	PENDING,
	SENDING,
	PUBLISHED,
	FAILED   // DLQ 상태
}