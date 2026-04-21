package jabaclass.product.infrastructure.outbox;

import jabaclass.product.config.KafkaTopicConfig;

public enum EsEventType {
	ES_SAVE(KafkaTopicConfig.PRODUCT_ES_TOPIC),
	ES_DELETE(KafkaTopicConfig.PRODUCT_ES_TOPIC);

	private final String topic;

	EsEventType(String topic) {
		this.topic = topic;
	}

	public String getTopic() {
		return topic;
	}
}
