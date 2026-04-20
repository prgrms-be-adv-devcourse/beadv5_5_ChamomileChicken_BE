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
import jabaclass.order.infrastructure.client.payment.dto.InternalRefundResponseDto;
import jabaclass.order.infrastructure.kafka.product.dto.OrderRefundedEvent;
import jabaclass.order.infrastructure.kafka.settlement.dto.SettlementRefundCompletedEvent;
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

	// [핸들러] PG 환불 성공 후 OrderService(동기)에서 호출
	// Order REFUNDED + 재고 복구(Product) + 예치금 복구(User) 이벤트를 같은 트랜잭션으로 커밋
	@Transactional
	public void onSuccess(UUID orderId, InternalRefundResponseDto refundResponse) {
		Order order = orderRepository.findById(orderId)
			.orElseThrow(() -> new BusinessException(OrderErrorCode.ORDER_NOT_FOUND));

		// 상태 가드: 중복 호출 방어 (동기/비동기 두 경로 모두 진입 가능)
		if (order.getStatus() == OrderStatus.REFUNDED) {
			return;
		}

		order.refund();

		publishRefundSuccessEvents(order, refundResponse);
	}

	private void publishRefundSuccessEvents(Order order, InternalRefundResponseDto refundResponse) {
		outboxRepository.save(OutboxEvent.create(
			"ORDER",
			order.getId().toString(),
			EventType.ORDER_REFUNDED,
			toJson(new OrderRefundedEvent(UUID.randomUUID(), order.getId(), order.getProductUserId()))
		));

		if (refundResponse.depositRefundAmount() != null && refundResponse.depositRefundAmount().signum() > 0) {
			outboxRepository.save(OutboxEvent.create(
				"ORDER",
				order.getId().toString(),
				EventType.ORDER_DEPOSIT_REFUND_REQUESTED,
				toJson(new DepositRefundRequestedEvent(
					UUID.randomUUID(),
					order.getId(),
					order.getUserId(),
					refundResponse.depositRefundAmount()
				))
			));
		}

		outboxRepository.save(OutboxEvent.create(
			"ORDER",
			order.getId().toString(),
			EventType.SETTLEMENT_REFUND_COMPLETED,
			toJson(new SettlementRefundCompletedEvent(
				UUID.randomUUID(),
				order.getId(),
				refundResponse.paymentId(),
				refundResponse.refundId(),
				order.getSellerId(),
				refundResponse.productId(),
				refundResponse.totalRefundAmount(),
				refundResponse.occurredAt()
			))
		));
	}

	private String toJson(Object obj) {
		try {
			objectMapper.findAndRegisterModules();
			return objectMapper.writeValueAsString(obj);
		} catch (Exception e) {
			throw new RuntimeException("Outbox 직렬화 실패", e);
		}
	}
}
