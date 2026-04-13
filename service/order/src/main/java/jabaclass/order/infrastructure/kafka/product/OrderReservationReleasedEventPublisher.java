package jabaclass.order.infrastructure.kafka.product;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import jabaclass.order.infrastructure.kafka.product.dto.OrderReservationReleasedEvent;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class OrderReservationReleasedEventPublisher {

	public static final String TOPIC = "order.reservation.released";

	private final KafkaTemplate<String, String> kafkaTemplate;
	private final ObjectMapper objectMapper;

	public void publish(OrderReservationReleasedEvent event) {
		try {
			String payload = objectMapper.writeValueAsString(event);
			kafkaTemplate.send(TOPIC, event.productUserId().toString(), payload);
		} catch (Exception e) {
			throw new RuntimeException("예약 해제 이벤트 발행 실패", e);
		}
	}
}