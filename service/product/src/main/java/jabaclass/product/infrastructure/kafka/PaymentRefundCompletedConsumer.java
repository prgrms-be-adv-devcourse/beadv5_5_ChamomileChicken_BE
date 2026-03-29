package jabaclass.product.infrastructure.kafka;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import jabaclass.product.application.usecase.ScheduleUseCase;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class PaymentRefundCompletedConsumer {

	private final ScheduleUseCase scheduleUseCase;
	private final ObjectMapper objectMapper;

	@KafkaListener(
		topics = "payment.refund.completed",
		groupId = "product-service"
	)
	public void consume(String message) {
		try {
			PaymentRefundCompletedEvent event = objectMapper.readValue(message, PaymentRefundCompletedEvent.class);
			scheduleUseCase.refundReservation(event.productUserId());
		} catch (Exception e) {
			throw new RuntimeException("상품 환불 이벤트 처리 실패", e);
		}
	}
}
