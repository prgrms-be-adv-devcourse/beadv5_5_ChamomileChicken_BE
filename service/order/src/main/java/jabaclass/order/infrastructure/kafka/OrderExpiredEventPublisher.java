package jabaclass.order.infrastructure.kafka;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class OrderExpiredEventPublisher {

	public static final String TOPIC = "order.expired";

	private final KafkaTemplate<String, String> kafkaTemplate;
	private final ObjectMapper objectMapper;

	public void publish(OrderExpiredEvent event) {
		try {
			kafkaTemplate.send(TOPIC, event.orderId().toString(), objectMapper.writeValueAsString(event));
		} catch (Exception e) {
			throw new RuntimeException("OrderExpired 이벤트 발행 실패", e);
		}
	}
}
