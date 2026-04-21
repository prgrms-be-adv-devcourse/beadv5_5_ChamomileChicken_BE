package jabaclass.settlement.infrastructure.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

	public static final String TOPIC = "settlement.events";
	public static final String DLQ_TOPIC = "settlement.events.dlq";

	@Bean
	public NewTopic settlementEventsTopic() {
		return TopicBuilder.name(TOPIC)
			.partitions(3)
			.replicas(1)
			.build();
	}

	@Bean
	public NewTopic settlementEventsDlqTopic() {
		return TopicBuilder.name(DLQ_TOPIC)
			.partitions(3)
			.replicas(1)
			.build();
	}
}
