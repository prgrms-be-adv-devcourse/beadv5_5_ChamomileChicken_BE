/*
package jabaclass.payment.infrastructure.kafka;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class PaymentFailedEventPublisher {

	public static final String TOPIC = "payment.failed";

	private final KafkaTemplate<String, String> kafkaTemplate;
	private final ObjectMapper objectMapper;

	public void publish(PaymentFailedEvent event) {
		try {
			String payload = objectMapper.writeValueAsString(event);
			kafkaTemplate.send(TOPIC, event.orderId().toString(), payload);
		} catch (Exception e) {
			throw new RuntimeException("결제 실패 이벤트 발행 실패", e);
		}
	}
}
*/
