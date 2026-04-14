package jabaclass.payment.infrastructure.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PaymentRefundCompletedEventPublisher {

	public static final String TOPIC = "payment.refund.completed";

	private final KafkaTemplate<String, String> kafkaTemplate;
	private final ObjectMapper objectMapper;

	public void publish(PaymentRefundCompletedEvent event) {
		try {
			String payload = objectMapper.writeValueAsString(event);
			kafkaTemplate.send(TOPIC, event.orderId().toString(), payload);
		} catch (Exception e) {
			throw new RuntimeException("환불 완료 이벤트 발행 실패", e);
		}
	}
}
