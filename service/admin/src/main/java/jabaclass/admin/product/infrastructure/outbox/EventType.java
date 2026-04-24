package jabaclass.admin.product.infrastructure.outbox;

public enum EventType {
	PRODUCT_FORCE_DOWN("admin.product"),
	USER_SELLER_APPROVED("settlement.events");

	private final String topic;

	EventType(String topic) {
		this.topic = topic;
	}

	public String getTopic() {
		return topic;
	}
}
