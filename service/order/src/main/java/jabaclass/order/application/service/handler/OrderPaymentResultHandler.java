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

	// [핸들러] PAYMENT_COMPLETED 수신 후 호출 — Order PAID + Outbox 저장을 원자적으로 커밋
	@Transactional
	public void onSuccess(UUID eventId, UUID orderId) {
		// processed_events로 중복 이벤트 차단 (Kafka at-least-once 방어)
		if (eventId != null && processedEventRepository.existsById(eventId)) {
			return;
		}
		Order order = findOrderOrThrow(orderId);
		// 상태 가드: eventId 없는 경로 또는 동시성 race condition 방어
		if (order.getStatus() == OrderStatus.PAID) {
			return;
		}
		order.pay();
		UUID productUserId = requireProductUserId(order);
		// Product 서비스로 재고 확정 이벤트 발행 — order.pay()와 같은 트랜잭션
		outboxRepository.save(OutboxEvent.create(
			"ORDER",
			orderId.toString(),
			EventType.ORDER_RESERVATION_CONFIRMED,
			toJson(new OrderReservationConfirmedEvent(UUID.randomUUID(), order.getId(), productUserId))
		));
		if (eventId != null) {
			processedEventRepository.save(ProcessedEvent.of(eventId));
		}
	}

	// [핸들러] PAYMENT_FAILED 수신 후 호출 — Saga 보상 트랜잭션
	// Order FAILED + 재고 복구(Product) + 예치금 복구(User) 이벤트를 같은 트랜잭션으로 커밋
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
		// 보상 1: Product 서비스로 재고 예약 해제
		outboxRepository.save(OutboxEvent.create(
			"ORDER",
			orderId.toString(),
			EventType.ORDER_RESERVATION_RELEASED,
			toJson(new OrderReservationReleasedEvent(UUID.randomUUID(), order.getId(), productUserId))
		));
		// 보상 2: User 서비스로 예치금 복구 (예치금을 사용한 경우에만)
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
