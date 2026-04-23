package jabaclass.order.common.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

	public static final String TOPIC = "order.events";
	public static final String DLQ_TOPIC = "order.events.dlq";
	public static final String PAYMENT_DLQ_TOPIC = "payment.events.dlq";
	public static final String SETTLEMENT_TOPIC = "settlement.events";

	@Bean
	public NewTopic orderEventsTopic() {
		return TopicBuilder.name(TOPIC)
			.partitions(3)
			.replicas(1)
			.build();
	}

	@Bean
	public NewTopic orderEventsDlqTopic() {
		return TopicBuilder.name(DLQ_TOPIC)
			.partitions(3)
			.replicas(1)
			.build();
	}

	@Bean
	public NewTopic paymentEventsDlqTopic() {
		return TopicBuilder.name(PAYMENT_DLQ_TOPIC)
			.partitions(3)
			.replicas(1)
			.build();
	}

	@Bean
	public NewTopic settlementEventsTopic() {
		return TopicBuilder.name(SETTLEMENT_TOPIC)
			.partitions(3)
			.replicas(1)
			.build();
	}
}
