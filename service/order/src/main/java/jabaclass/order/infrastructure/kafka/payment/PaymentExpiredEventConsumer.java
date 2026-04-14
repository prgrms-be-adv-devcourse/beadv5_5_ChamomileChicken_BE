package jabaclass.order.infrastructure.kafka.payment;

import com.fasterxml.jackson.databind.ObjectMapper;

import jabaclass.order.application.port.internal.OrderUseCase;
import jabaclass.order.infrastructure.kafka.payment.dto.PaymentExpiredEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentExpiredEventConsumer {

	public static final String TOPIC = "payment.expired";

	private final OrderUseCase orderUseCase;
	private final ObjectMapper objectMapper;

	@KafkaListener(topics = TOPIC, groupId = "order-service")
	public void consume(String message) {
		try {
			PaymentExpiredEvent event = objectMapper.readValue(message, PaymentExpiredEvent.class);
			orderUseCase.expireOrder(event.orderId(), event.depositAmount());
			log.info("payment.expired 이벤트 처리 완료. orderId={}", event.orderId());
		} catch (Exception e) {
			log.error("payment.expired 처리 실패. message={}", message, e);
			throw new RuntimeException("결제 만료 이벤트 처리 실패", e);
		}
	}
}