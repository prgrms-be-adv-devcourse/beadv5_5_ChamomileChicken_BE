package jabaclass.payment.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

import jabaclass.payment.infrastructure.kafka.PaymentRefundCompletedEventPublisher;

@Configuration
public class KafkaTopicConfig {

	@Bean
	public NewTopic paymentRefundCompletedTopic() {
		return TopicBuilder.name(PaymentRefundCompletedEventPublisher.TOPIC)
			.partitions(3)
			.replicas(1)
			.build();
	}
	
}
