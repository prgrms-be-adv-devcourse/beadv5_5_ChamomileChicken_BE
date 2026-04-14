package jabaclass.order.infrastructure.kafka;

import java.math.BigDecimal;
import java.util.UUID;

import jabaclass.order.domain.model.Order;
import jabaclass.order.infrastructure.kafka.product.dto.OrderReservationConfirmedEvent;
import jabaclass.order.infrastructure.kafka.product.OrderReservationConfirmedEventPublisher;
import jabaclass.order.infrastructure.kafka.product.dto.OrderReservationReleasedEvent;
import jabaclass.order.infrastructure.kafka.product.OrderReservationReleasedEventPublisher;
import jabaclass.order.infrastructure.kafka.user.DepositRefundRequestedEventPublisher;
import jabaclass.order.infrastructure.kafka.user.dto.DepositRefundRequestedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OrderEventPublisher {

	private final OrderReservationConfirmedEventPublisher reservationConfirmedEventPublisher;
	private final OrderReservationReleasedEventPublisher reservationReleasedEventPublisher;
	private final DepositRefundRequestedEventPublisher depositRefundRequestedEventPublisher;
	private final OrderExpiredEventPublisher orderExpiredEventPublisher;

	public void publishReservationConfirmed(UUID productUserId) {
		reservationConfirmedEventPublisher.publish(new OrderReservationConfirmedEvent(UUID.randomUUID(), productUserId));
	}

	public void publishReservationReleased(UUID productUserId) {
		reservationReleasedEventPublisher.publish(new OrderReservationReleasedEvent(UUID.randomUUID(), productUserId));
	}

	public void publishDepositRefundRequested(Order order, BigDecimal depositAmount) {
		if (depositAmount == null || depositAmount.signum() == 0) {
			return;
		}
		depositRefundRequestedEventPublisher.publish(
			new DepositRefundRequestedEvent(UUID.randomUUID(), order.getId(), order.getUserId(), depositAmount)
		);
	}

	public void publishOrderExpired(Order order, BigDecimal depositAmount) {
		orderExpiredEventPublisher.publish(
			new OrderExpiredEvent(UUID.randomUUID(), order.getId(), order.getUserId(), depositAmount)
		);
	}
}