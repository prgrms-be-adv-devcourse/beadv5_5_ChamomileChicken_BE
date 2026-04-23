package jabaclass.product.infrastructure.kafka;

public enum ProductEventType {
	PRODUCT_AI_SYNCED("product.events"),
	PRODUCT_DELETED("product.events"),
	PRODUCT_VIEWED("product.events");

	private final String topic;

	ProductEventType(String topic) {
		this.topic = topic;
	}

	public String getTopic() {
		return topic;
	}
}
