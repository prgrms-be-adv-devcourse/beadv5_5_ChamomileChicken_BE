package jabaclass.product.infrastructure.kafka.order;

import java.util.UUID;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import jabaclass.product.application.usecase.ScheduleUseCase;
import jabaclass.product.domain.model.status.ReservationStatus;
import jabaclass.product.infrastructure.idempotency.ProcessedEvent;
import jabaclass.product.infrastructure.idempotency.ProcessedEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventHandler {

	private final ScheduleUseCase scheduleUseCase;
	private final ProcessedEventRepository processedEventRepository;

	@Transactional
	public void handleReservationConfirmed(UUID eventId, UUID productUserId) {
		if (eventId != null && processedEventRepository.existsById(eventId)) {
			return;
		}
		scheduleUseCase.reservationCompleted(productUserId);
		if (eventId != null) {
			processedEventRepository.save(ProcessedEvent.of(eventId));
		}
		log.info("ORDER_RESERVATION_CONFIRMED 처리 완료. productUserId={}", productUserId);
	}

	@Transactional
	public void handleReservationReleased(UUID eventId, UUID productUserId) {
		if (eventId != null && processedEventRepository.existsById(eventId)) {
			return;
		}
		scheduleUseCase.restoringInventory(productUserId, ReservationStatus.RELEASED);
		if (eventId != null) {
			processedEventRepository.save(ProcessedEvent.of(eventId));
		}
		log.info("ORDER_RESERVATION_RELEASED 처리 완료. productUserId={}", productUserId);
	}
}
