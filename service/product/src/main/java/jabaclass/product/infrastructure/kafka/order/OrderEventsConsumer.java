package jabaclass.product.infrastructure.kafka.order;

import java.nio.charset.StandardCharsets;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventsConsumer {

	private final OrderEventHandler orderEventHandler;
	private final ObjectMapper objectMapper;

	@KafkaListener(topics = "order.events", groupId = "product-service")
	public void consume(ConsumerRecord<String, String> record) {
		String eventType = new String(record.headers().lastHeader("eventType").value(), StandardCharsets.UTF_8);
		String message = record.value();

		try {
			switch (eventType) {
				case "ORDER_RESERVATION_CONFIRMED" -> {
					OrderReservationConfirmedEvent event = objectMapper.readValue(message, OrderReservationConfirmedEvent.class);
					orderEventHandler.handleReservationConfirmed(event.eventId(), event.productUserId());
				}
				case "ORDER_RESERVATION_RELEASED" -> {
					OrderReservationReleasedEvent event = objectMapper.readValue(message, OrderReservationReleasedEvent.class);
					orderEventHandler.handleReservationReleased(event.eventId(), event.productUserId());
				}
				case "ORDER_REFUNDED" -> {
					OrderRefundedEvent event = objectMapper.readValue(message, OrderRefundedEvent.class);
					orderEventHandler.handleOrderRefunded(event.eventId(), event.productUserId());
				}
				default -> log.warn("알 수 없는 eventType: {}", eventType);
			}
		} catch (Exception e) {
			log.error("order.events 처리 실패. eventType={}, message={}", eventType, message, e);
			throw new RuntimeException("order.events 이벤트 처리 실패: " + eventType, e);
		}
	}
}
