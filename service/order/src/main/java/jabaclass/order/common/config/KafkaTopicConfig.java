package jabaclass.order.common.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

	public static final String TOPIC = "order.events";

	@Bean
	public NewTopic orderEventsTopic() {
		return TopicBuilder.name(TOPIC)
			.partitions(3)
			.replicas(1)
			.build();
	}
}