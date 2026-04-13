package jabaclass.product.infrastructure.kafka.fromorder;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import jabaclass.product.application.usecase.ScheduleUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderReservationConfirmedConsumer {

	public static final String TOPIC = "order.reservation.confirmed";

	private final ScheduleUseCase scheduleUseCase;
	private final ObjectMapper objectMapper;

	@KafkaListener(
		topics = TOPIC,
		groupId = "product-service"
	)
	public void consume(String message) {
		try {
			OrderReservationConfirmedEvent event =
				objectMapper.readValue(message, OrderReservationConfirmedEvent.class);

			//	scheduleUseCase.confirmReservation(event.productUserId());
			scheduleUseCase.reservationCompleted(event.productUserId());

			log.info("order.reservation.confirmed 처리 완료. productUserId={}", event.productUserId());
		} catch (Exception e) {
			log.error("order.reservation.confirmed 처리 실패. message={}", message, e);
			throw new RuntimeException("예약 확정 이벤트 처리 실패", e);
		}
	}
}