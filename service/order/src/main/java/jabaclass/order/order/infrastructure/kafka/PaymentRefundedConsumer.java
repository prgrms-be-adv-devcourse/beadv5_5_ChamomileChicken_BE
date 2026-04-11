package jabaclass.order.order.infrastructure.kafka;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import jabaclass.order.order.application.usecase.OrderUseCase;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class PaymentRefundedConsumer {

	private final OrderUseCase orderUseCase;
	private final ObjectMapper objectMapper;

	@KafkaListener(
		topics = "payment.refunded",
		groupId = "order-service"
	)
	public void consume(String message) {
		try {
			PaymentRefundedEvent event = objectMapper.readValue(message, PaymentRefundedEvent.class);
			orderUseCase.refund(event.orderId());
		} catch (Exception e) {
			throw new RuntimeException("환불 완료 이벤트 처리 실패", e);
		}
	}
}
