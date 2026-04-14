package jabaclass.product.infrastructure.kafka.fromorder;

import java.nio.charset.StandardCharsets;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import jabaclass.product.application.usecase.ScheduleUseCase;
import jabaclass.product.domain.model.status.ReservationStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventsConsumer {

	private final ScheduleUseCase scheduleUseCase;
	private final ObjectMapper objectMapper;

	@KafkaListener(topics = "order.events", groupId = "product-service")
	public void consume(ConsumerRecord<String, String> record) {
		String eventType = new String(record.headers().lastHeader("eventType").value(), StandardCharsets.UTF_8);
		String message = record.value();

		try {
			switch (eventType) {
				case "ORDER_RESERVATION_CONFIRMED" -> handleReservationConfirmed(message);
				case "ORDER_RESERVATION_RELEASED" -> handleReservationReleased(message);
				default -> log.warn("알 수 없는 eventType: {}", eventType);
			}
		} catch (Exception e) {
			log.error("order.events 처리 실패. eventType={}, message={}", eventType, message, e);
			throw new RuntimeException("order.events 이벤트 처리 실패: " + eventType, e);
		}
	}

	private void handleReservationConfirmed(String message) throws Exception {
		OrderReservationConfirmedEvent event = objectMapper.readValue(message, OrderReservationConfirmedEvent.class);
		scheduleUseCase.reservationCompleted(event.productUserId());
		log.info("ORDER_RESERVATION_CONFIRMED 처리 완료. productUserId={}", event.productUserId());
	}

	private void handleReservationReleased(String message) throws Exception {
		OrderReservationReleasedEvent event = objectMapper.readValue(message, OrderReservationReleasedEvent.class);
		scheduleUseCase.restoringInventory(event.productUserId(), ReservationStatus.RELEASED);
		log.info("ORDER_RESERVATION_RELEASED 처리 완료. productUserId={}", event.productUserId());
	}
}