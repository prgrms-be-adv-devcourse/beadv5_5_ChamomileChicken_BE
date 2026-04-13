package jabaclass.order.infrastructure.kafka;

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
		reservationConfirmedEventPublisher.publish(new OrderReservationConfirmedEvent(productUserId));
	}

	public void publishReservationReleased(UUID productUserId) {
		reservationReleasedEventPublisher.publish(new OrderReservationReleasedEvent(productUserId));
	}

	public void publishDepositRefundRequested(Order order) {
		if (order.getDepositAmount().signum() == 0) {
			return;
		}
		depositRefundRequestedEventPublisher.publish(
			new DepositRefundRequestedEvent(order.getId(), order.getUserId(), order.getDepositAmount())
		);
	}

	public void publishOrderExpired(Order order) {
		orderExpiredEventPublisher.publish(
			new OrderExpiredEvent(order.getId(), order.getUserId(), order.getDepositAmount())
		);
	}
}