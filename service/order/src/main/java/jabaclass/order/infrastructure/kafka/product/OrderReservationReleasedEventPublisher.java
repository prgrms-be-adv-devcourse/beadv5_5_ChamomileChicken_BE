package jabaclass.order.infrastructure.kafka.product;

import java.nio.charset.StandardCharsets;

import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import jabaclass.order.infrastructure.kafka.product.dto.OrderReservationReleasedEvent;
import jabaclass.order.common.config.KafkaTopicConfig;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class OrderReservationReleasedEventPublisher {

	public static final String EVENT_TYPE = "ORDER_RESERVATION_RELEASED";

	private final KafkaTemplate<String, String> kafkaTemplate;
	private final ObjectMapper objectMapper;

	public void publish(OrderReservationReleasedEvent event) {
		try {
			String payload = objectMapper.writeValueAsString(event);
			ProducerRecord<String, String> record = new ProducerRecord<>(
				KafkaTopicConfig.TOPIC,
				event.productUserId().toString(),
				payload
			);
			record.headers().add("eventType", EVENT_TYPE.getBytes(StandardCharsets.UTF_8));
			kafkaTemplate.send(record);
		} catch (Exception e) {
			throw new RuntimeException("예약 해제 이벤트 발행 실패", e);
		}
	}
}