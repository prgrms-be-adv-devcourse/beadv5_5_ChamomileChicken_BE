package jabaclass.order.infrastructure.kafka.payment;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import jabaclass.order.application.port.in.OrderUseCase;
import jabaclass.order.domain.model.PaymentResultStatus;
import jabaclass.order.infrastructure.kafka.payment.dto.PaymentFailedEvent;
import jabaclass.order.presentation.dto.request.UpdateOrderPaymentStatusRequestDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentFailedConsumer {

	public static final String TOPIC = "payment.failed";

	private final OrderUseCase orderUseCase;
	private final ObjectMapper objectMapper;

	@KafkaListener(
		topics = TOPIC,
		groupId = "order-service"
	)
	public void consume(String message) {
		try {
			PaymentFailedEvent event = objectMapper.readValue(message, PaymentFailedEvent.class);

			orderUseCase.updatePaymentStatus(
				event.orderId(),
				new UpdateOrderPaymentStatusRequestDto(PaymentResultStatus.FAILED)
			);

			log.info("payment.failed 이벤트 처리 완료. orderId={}", event.orderId());
		} catch (Exception e) {
			log.error("payment.failed 처리 실패. message={}", message, e);
			throw new RuntimeException("결제 실패 이벤트 처리 실패", e);
		}
	}
}