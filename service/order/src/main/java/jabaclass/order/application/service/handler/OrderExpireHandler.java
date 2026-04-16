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
import jabaclass.order.infrastructure.idempotency.ProcessedEvent;
import jabaclass.order.infrastructure.idempotency.ProcessedEventRepository;
import jabaclass.order.infrastructure.kafka.product.dto.OrderReservationReleasedEvent;
import jabaclass.order.infrastructure.kafka.user.dto.OrderExpiredEvent;
import jabaclass.order.infrastructure.outbox.EventType;
import jabaclass.order.infrastructure.outbox.OutboxEvent;
import jabaclass.order.infrastructure.outbox.OutboxRepository;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class OrderExpireHandler {

	private final OrderRepository orderRepository;
	private final OutboxRepository outboxRepository;
	private final ProcessedEventRepository processedEventRepository;
	private final ObjectMapper objectMapper;

	@Transactional
	public void expire(UUID eventId, UUID orderId, BigDecimal depositAmount) {
		if (eventId != null && processedEventRepository.existsById(eventId)) {
			return;
		}
		Order order = findOrderOrThrow(orderId);
		if (order.getStatus() == OrderStatus.EXPIRED) {
			return;
		}
		order.expire();
		UUID productUserId = requireProductUserId(order);
		outboxRepository.save(OutboxEvent.create(
			"ORDER",
			productUserId.toString(),
			EventType.ORDER_RESERVATION_RELEASED,
			toJson(new OrderReservationReleasedEvent(UUID.randomUUID(), order.getId(), productUserId))
		));
		outboxRepository.save(OutboxEvent.create(
			"ORDER",
			order.getId().toString(),
			EventType.ORDER_EXPIRED,
			toJson(new OrderExpiredEvent(UUID.randomUUID(), order.getId(), order.getUserId(), depositAmount))
		));
		if (eventId != null) {
			processedEventRepository.save(ProcessedEvent.of(eventId));
		}
	}

	private Order findOrderOrThrow(UUID orderId) {
		return orderRepository.findById(orderId)
			.orElseThrow(() -> new BusinessException(OrderErrorCode.ORDER_NOT_FOUND));
	}

	private UUID requireProductUserId(Order order) {
		if (order.getProductUserId() == null) {
			throw new BusinessException(OrderErrorCode.ORDER_PRODUCT_USER_ID_REQUIRED);
		}
		return order.getProductUserId();
	}

	private String toJson(Object obj) {
		try {
			return objectMapper.writeValueAsString(obj);
		} catch (Exception e) {
			throw new RuntimeException("Outbox 직렬화 실패", e);
		}
	}
}
