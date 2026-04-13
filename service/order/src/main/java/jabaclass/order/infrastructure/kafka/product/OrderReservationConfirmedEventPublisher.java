package jabaclass.order.infrastructure.kafka.product;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import jabaclass.order.infrastructure.kafka.product.dto.OrderReservationConfirmedEvent;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class OrderReservationConfirmedEventPublisher {

	public static final String TOPIC = "order.reservation.confirmed";

	private final KafkaTemplate<String, String> kafkaTemplate;
	private final ObjectMapper objectMapper;

	public void publish(OrderReservationConfirmedEvent event) {
		try {
			String payload = objectMapper.writeValueAsString(event);
			kafkaTemplate.send(TOPIC, event.productUserId().toString(), payload);
		} catch (Exception e) {
			throw new RuntimeException("예약 확정 이벤트 발행 실패", e);
		}
	}
}