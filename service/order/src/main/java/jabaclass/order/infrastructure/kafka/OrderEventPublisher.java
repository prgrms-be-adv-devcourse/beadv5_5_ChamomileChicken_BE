package jabaclass.order.infrastructure.kafka;

import java.math.BigDecimal;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import jabaclass.order.domain.model.Order;
import jabaclass.order.infrastructure.kafka.product.dto.OrderReservationConfirmedEvent;
import jabaclass.order.infrastructure.kafka.product.dto.OrderReservationReleasedEvent;
import jabaclass.order.infrastructure.kafka.user.dto.DepositRefundRequestedEvent;
import jabaclass.order.infrastructure.kafka.user.dto.OrderExpiredEvent;
import jabaclass.order.infrastructure.outbox.EventType;
import jabaclass.order.infrastructure.outbox.OutboxEvent;
import jabaclass.order.infrastructure.outbox.OutboxRepository;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class OrderEventPublisher {

	private final OutboxRepository outboxRepository;
	private final ObjectMapper objectMapper;

	public void publishReservationConfirmed(UUID productUserId) {
		OrderReservationConfirmedEvent event = new OrderReservationConfirmedEvent(UUID.randomUUID(), productUserId);
		outboxRepository.save(OutboxEvent.create(
			"ORDER",
			productUserId.toString(),
			EventType.ORDER_RESERVATION_CONFIRMED,
			toJson(event)
		));
	}

	public void publishReservationReleased(UUID productUserId) {
		OrderReservationReleasedEvent event = new OrderReservationReleasedEvent(UUID.randomUUID(), productUserId);
		outboxRepository.save(OutboxEvent.create(
			"ORDER",
			productUserId.toString(),
			EventType.ORDER_RESERVATION_RELEASED,
			toJson(event)
		));
	}

	public void publishDepositRefundRequested(Order order, BigDecimal depositAmount) {
		if (depositAmount == null || depositAmount.signum() == 0) {
			return;
		}
		DepositRefundRequestedEvent event = new DepositRefundRequestedEvent(
			UUID.randomUUID(), order.getId(), order.getUserId(), depositAmount
		);
		outboxRepository.save(OutboxEvent.create(
			"ORDER",
			order.getId().toString(),
			EventType.ORDER_DEPOSIT_REFUND_REQUESTED,
			toJson(event)
		));
	}

	public void publishOrderExpired(Order order, BigDecimal depositAmount) {
		OrderExpiredEvent event = new OrderExpiredEvent(
			UUID.randomUUID(), order.getId(), order.getUserId(), depositAmount
		);
		outboxRepository.save(OutboxEvent.create(
			"ORDER",
			order.getId().toString(),
			EventType.ORDER_EXPIRED,
			toJson(event)
		));
	}

	private String toJson(Object obj) {
		try {
			return objectMapper.writeValueAsString(obj);
		} catch (Exception e) {
			throw new RuntimeException("Outbox 직렬화 실패", e);
		}
	}
}