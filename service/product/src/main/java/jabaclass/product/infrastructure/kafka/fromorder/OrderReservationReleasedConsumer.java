package jabaclass.product.infrastructure.kafka.fromorder;

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
public class OrderReservationReleasedConsumer {

	public static final String TOPIC = "order.reservation.released";

	private final ScheduleUseCase scheduleUseCase;
	private final ObjectMapper objectMapper;

	@KafkaListener(
		topics = TOPIC,
		groupId = "product-service"
	)
	public void consume(String message) {
		try {
			OrderReservationReleasedEvent event =
				objectMapper.readValue(message, OrderReservationReleasedEvent.class);

			//	scheduleUseCase.releaseReservation(event.productUserId());
			// 아래 서비스를 확인해주세요. 예약 해제 기준으로 했습니다.
			scheduleUseCase.restoringInventory(event.productUserId(), ReservationStatus.RELEASED);

			log.info("order.reservation.released 처리 완료. productUserId={}", event.productUserId());
		} catch (Exception e) {
			log.error("order.reservation.released 처리 실패. message={}", message, e);
			throw new RuntimeException("예약 해제 이벤트 처리 실패", e);
		}
	}
}