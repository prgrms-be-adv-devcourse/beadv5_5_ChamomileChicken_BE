package jabaclass.payment.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

	public static final String TOPIC = "payment.events";

	@Bean
	public NewTopic paymentEventsTopic() {
		return TopicBuilder.name(TOPIC)
			.partitions(3)
			.replicas(1)
			.build();
	}
}
