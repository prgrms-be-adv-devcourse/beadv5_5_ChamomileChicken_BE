package jabaclass.product.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

	public static final String PRODUCT_ES_TOPIC = "product.es.index";

	@Bean
	public NewTopic productEsTopic() {
		return TopicBuilder.name(PRODUCT_ES_TOPIC)
			.partitions(3)
			.replicas(1)
			.build();
	}
}
