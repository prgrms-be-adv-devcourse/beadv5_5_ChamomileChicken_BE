package jabaclass.payment.infrastructure.outbox;

public enum EventType {
	PAYMENT_COMPLETED("payment.events"),
	PAYMENT_FAILED("payment.events"),
	PAYMENT_REFUNDED("payment.events"),
	PAYMENT_EXPIRED("payment.events");

	private final String topic;

	EventType(String topic) {
		this.topic = topic;
	}

	public String getTopic() {
		return topic;
	}
}
