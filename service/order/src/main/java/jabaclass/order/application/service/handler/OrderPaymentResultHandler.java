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
import jabaclass.order.infrastructure.kafka.product.dto.OrderReservationConfirmedEvent;
import jabaclass.order.infrastructure.kafka.product.dto.OrderReservationReleasedEvent;
import jabaclass.order.infrastructure.kafka.user.dto.DepositRefundRequestedEvent;
import jabaclass.order.infrastructure.outbox.EventType;
import jabaclass.order.infrastructure.outbox.OutboxEvent;
import jabaclass.order.infrastructure.outbox.OutboxRepository;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class OrderPaymentResultHandler {

	private final OrderRepository orderRepository;
	private final OutboxRepository outboxRepository;
	private final ProcessedEventRepository processedEventRepository;
	private final ObjectMapper objectMapper;

	@Transactional
	public void onSuccess(UUID eventId, UUID orderId) {
		// Kafka at-least-once로 인한 중복 이벤트 방어 (eventId 기반 멱등성)
		if (eventId != null && processedEventRepository.existsById(eventId)) {
			return;
		}
		Order order = findOrderOrThrow(orderId);
		// eventId 없는 경로(컨트롤러) 또는 동시성 race condition 방어
		if (order.getStatus() == OrderStatus.PAID) {
			return;
		}
		order.pay();
		UUID productUserId = requireProductUserId(order);
		// 재고 확정 이벤트 발행 — order.pay()와 같은 트랜잭션으로 원자성 보장
		outboxRepository.save(OutboxEvent.create(
			"ORDER",
			productUserId.toString(),
			EventType.ORDER_RESERVATION_CONFIRMED,
			toJson(new OrderReservationConfirmedEvent(UUID.randomUUID(), order.getId(), productUserId))
		));
		// 처리 완료 기록 — 다음 중복 수신 시 위 existsById에서 차단
		if (eventId != null) {
			processedEventRepository.save(ProcessedEvent.of(eventId));
		}
	}

	@Transactional
	public void onFailed(UUID eventId, UUID orderId, BigDecimal depositAmount) {
		if (eventId != null && processedEventRepository.existsById(eventId)) {
			return;
		}
		Order order = findOrderOrThrow(orderId);
		if (order.getStatus() == OrderStatus.FAILED || order.getStatus() == OrderStatus.EXPIRED) {
			return;
		}
		order.failPayment();
		UUID productUserId = requireProductUserId(order);
		outboxRepository.save(OutboxEvent.create(
			"ORDER",
			productUserId.toString(),
			EventType.ORDER_RESERVATION_RELEASED,
			toJson(new OrderReservationReleasedEvent(UUID.randomUUID(), order.getId(), productUserId))
		));
		if (depositAmount != null && depositAmount.signum() > 0) {
			outboxRepository.save(OutboxEvent.create(
				"ORDER",
				order.getId().toString(),
				EventType.ORDER_DEPOSIT_REFUND_REQUESTED,
				toJson(new DepositRefundRequestedEvent(UUID.randomUUID(), order.getId(), order.getUserId(), depositAmount))
			));
		}
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
