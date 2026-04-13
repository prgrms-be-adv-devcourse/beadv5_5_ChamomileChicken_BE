package jabaclass.order.infrastructure.kafka.payment;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import jabaclass.order.application.port.internal.OrderUseCase;
import jabaclass.order.infrastructure.kafka.payment.dto.PaymentRefundCompletedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentRefundCompletedEventConsumer {

	public static final String TOPIC = "payment.refund.completed";

	private final OrderUseCase orderUseCase;
	private final ObjectMapper objectMapper;

	@KafkaListener(
		topics = TOPIC,
		groupId = "order-service"
	)
	public void consume(String message) {
		// TODO: 환불 구현 예정
		// 1. payment.refund.completed 수신 { orderId }
		// 2. Order.status: PAID → REFUNDED
		// 3. order.reservation.refunded 발행 → ProductService 재고/상태 복구

		try {
			PaymentRefundCompletedEvent event = objectMapper.readValue(message, PaymentRefundCompletedEvent.class);
			log.info("payment.refund.completed 수신. orderId={} (환불 미구현)", event.orderId());

			// orderUseCase.refund(event.orderId());
		} catch (Exception e) {
			log.error("payment.refund.completed 처리 실패. message={}", message, e);
			throw new RuntimeException("환불 완료 이벤트 처리 실패", e);
		}
	}
}
