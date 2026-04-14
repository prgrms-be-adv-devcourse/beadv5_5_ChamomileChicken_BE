package jabaclass.payment.infrastructure.outbox;

public enum EventType {
	PAYMENT_COMPLETED("payment.completed"),
	PAYMENT_FAILED("payment.failed"),
	PAYMENT_REFUNDED("payment.refunded"),
	PAYMENT_EXPIRED("payment.expired");

	private final String topic;

	EventType(String topic) {
		this.topic = topic;
	}

	public String getTopic() {
		return topic;
	}
}
