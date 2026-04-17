package jabaclass.order.application.service.handler;

import java.math.BigDecimal;
import java.util.UUID;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;

import jabaclass.order.application.exception.OrderErrorCode;
import jabaclass.order.common.error.BusinessException;
import jabaclass.order.domain.model.Order;
import jabaclass.order.domain.model.OrderStatus;
import jabaclass.order.domain.repository.OrderRepository;
import jabaclass.order.infrastructure.kafka.product.dto.OrderRefundedEvent;
import jabaclass.order.infrastructure.kafka.user.dto.DepositRefundRequestedEvent;
import jabaclass.order.infrastructure.outbox.EventType;
import jabaclass.order.infrastructure.outbox.OutboxEvent;
import jabaclass.order.infrastructure.outbox.OutboxRepository;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class OrderRefundHandler {

	private final OrderRepository orderRepository;
	private final OutboxRepository outboxRepository;
	private final ObjectMapper objectMapper;

	@Transactional
	public void onSuccess(UUID orderId, BigDecimal depositRefundAmount) {
		Order order = orderRepository.findById(orderId)
			.orElseThrow(() -> new BusinessException(OrderErrorCode.ORDER_NOT_FOUND));

		if (order.getStatus() == OrderStatus.REFUNDED) {
			return;
		}

		order.refund();

		// Product: CONFIRMED → REFUNDED
		outboxRepository.save(OutboxEvent.create(
			"ORDER",
			orderId.toString(),
			EventType.ORDER_REFUNDED,
			toJson(new OrderRefundedEvent(UUID.randomUUID(), orderId, order.getProductUserId()))
		));

		// User: 예치금 복구
		if (depositRefundAmount != null && depositRefundAmount.signum() > 0) {
			outboxRepository.save(OutboxEvent.create(
				"ORDER",
				orderId.toString(),
				EventType.ORDER_DEPOSIT_REFUND_REQUESTED,
				toJson(new DepositRefundRequestedEvent(UUID.randomUUID(), orderId, order.getUserId(), depositRefundAmount))
			));
		}
	}

	private String toJson(Object obj) {
		try {
			return objectMapper.writeValueAsString(obj);
		} catch (Exception e) {
			throw new RuntimeException("Outbox 직렬화 실패", e);
		}
	}
}