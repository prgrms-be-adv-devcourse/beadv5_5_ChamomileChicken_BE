package jabaclass.order.common.config;

import jabaclass.order.infrastructure.kafka.OrderExpiredEventPublisher;
import jabaclass.order.infrastructure.kafka.payment.PaymentCompletedConsumer;
import jabaclass.order.infrastructure.kafka.payment.PaymentFailedConsumer;
import jabaclass.order.infrastructure.kafka.payment.PaymentRefundCompletedConsumer;
import jabaclass.order.infrastructure.kafka.product.OrderReservationConfirmedEventPublisher;
import jabaclass.order.infrastructure.kafka.product.OrderReservationReleasedEventPublisher;
import jabaclass.order.infrastructure.kafka.user.DepositRefundRequestedEventPublisher;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

	@Bean
	public NewTopic paymentCompletedTopic() {
		return TopicBuilder.name(PaymentCompletedConsumer.TOPIC)
			.partitions(3)
			.replicas(1)
			.build();
	}

	@Bean
	public NewTopic paymentFailedTopic() {
		return TopicBuilder.name(PaymentFailedConsumer.TOPIC)
			.partitions(3)
			.replicas(1)
			.build();
	}

	@Bean
	public NewTopic paymentRefundCompletedTopic() {
		return TopicBuilder.name(PaymentRefundCompletedConsumer.TOPIC)
			.partitions(3)
			.replicas(1)
			.build();
	}

	@Bean
	public NewTopic orderReservationConfirmedTopic() {
		return TopicBuilder.name(OrderReservationConfirmedEventPublisher.TOPIC)
			.partitions(3)
			.replicas(1)
			.build();
	}

	@Bean
	public NewTopic orderReservationReleasedTopic() {
		return TopicBuilder.name(OrderReservationReleasedEventPublisher.TOPIC)
			.partitions(3)
			.replicas(1)
			.build();
	}

	@Bean
	public NewTopic orderDepositRefundRequestedTopic() {
		return TopicBuilder.name(DepositRefundRequestedEventPublisher.TOPIC)
			.partitions(3)
			.replicas(1)
			.build();
	}

	@Bean
	public NewTopic orderExpiredTopic() {
		return TopicBuilder.name(OrderExpiredEventPublisher.TOPIC)
			.partitions(3)
			.replicas(1)
			.build();
	}
}
