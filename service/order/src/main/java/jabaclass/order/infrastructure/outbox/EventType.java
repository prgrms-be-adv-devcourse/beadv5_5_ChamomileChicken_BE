package jabaclass.order.infrastructure.outbox;

public enum EventType {
	ORDER_RESERVATION_CONFIRMED("order.events"),
	ORDER_RESERVATION_RELEASED("order.events"),
	ORDER_DEPOSIT_REFUND_REQUESTED("order.events"),
	ORDER_EXPIRED("order.events"),
	ORDER_REFUNDED("order.events");

	private final String topic;

	EventType(String topic) {
		this.topic = topic;
	}

	public String getTopic() {
		return topic;
	}
}
