package jabaclass.order.infrastructure.kafka.user;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import jabaclass.order.infrastructure.kafka.user.dto.DepositRefundRequestedEvent;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class DepositRefundRequestedEventPublisher {

	public static final String TOPIC = "order.deposit.refund-requested";

	private final KafkaTemplate<String, String> kafkaTemplate;
	private final ObjectMapper objectMapper;

	public void publish(DepositRefundRequestedEvent event) {
		try {
			kafkaTemplate.send(TOPIC, event.orderId().toString(), objectMapper.writeValueAsString(event));
		} catch (Exception e) {
			throw new RuntimeException("예치금 복구 요청 이벤트 발행 실패", e);
		}
	}
}
