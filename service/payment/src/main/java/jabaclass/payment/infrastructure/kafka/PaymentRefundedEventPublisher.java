package jabaclass.payment.infrastructure.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PaymentRefundedEventPublisher {

	public static final String TOPIC = "payment.refunded";

	private final KafkaTemplate<String, String> kafkaTemplate;
	private final ObjectMapper objectMapper;

	public void publish(PaymentRefundedEvent event) {
		try {
			String payload = objectMapper.writeValueAsString(event);
			kafkaTemplate.send(TOPIC, event.orderId().toString(), payload);
		} catch (Exception e) {
			throw new RuntimeException("환불 완료 이벤트 발행 실패", e);
		}
	}
}
