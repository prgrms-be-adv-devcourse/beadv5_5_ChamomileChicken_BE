/*
package jabaclass.payment.infrastructure.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PaymentExpiredEventPublisher {

	public static final String TOPIC = "payment.expired";

	private final KafkaTemplate<String, String> kafkaTemplate;
	private final ObjectMapper objectMapper;

	public void publish(PaymentExpiredEvent event) {
		try {
			String payload = objectMapper.writeValueAsString(event);
			kafkaTemplate.send(TOPIC, event.orderId().toString(), payload);
		} catch (Exception e) {
			throw new RuntimeException("결제 만료 이벤트 발행 실패", e);
		}
	}
}*/
