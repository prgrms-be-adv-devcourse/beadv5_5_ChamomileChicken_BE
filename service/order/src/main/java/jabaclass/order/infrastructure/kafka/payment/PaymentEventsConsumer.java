package jabaclass.order.infrastructure.kafka.payment;

import java.nio.charset.StandardCharsets;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import jabaclass.order.application.port.internal.OrderUseCase;
import jabaclass.order.infrastructure.kafka.payment.dto.PaymentCompletedEvent;
import jabaclass.order.infrastructure.kafka.payment.dto.PaymentExpiredEvent;
import jabaclass.order.infrastructure.kafka.payment.dto.PaymentFailedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentEventsConsumer {

	private final OrderUseCase orderUseCase;
	private final ObjectMapper objectMapper;

	// Kafka at-least-once 특성상 같은 메시지가 중복 수신될 수 있음
	// 각 핸들러는 processed_events(eventId PK) 또는 상태 가드로 중복 처리 방어
	// 예외 throw 시 → KafkaConsumerConfig의 DefaultErrorHandler가 1초 간격 3회 재시도
	// 3회 초과 시 → payment.events.dlq 토픽으로 전송 (오프셋은 정상 커밋되어 다음 메시지 처리 계속)
	@KafkaListener(topics = "payment.events", groupId = "order-service")
	public void consume(ConsumerRecord<String, String> record) {
		String eventType = new String(record.headers().lastHeader("eventType").value(), StandardCharsets.UTF_8);
		String message = record.value();

		try {
			switch (eventType) {
				case "PAYMENT_COMPLETED" -> handlePaymentCompleted(message);
				case "PAYMENT_FAILED" -> handlePaymentFailed(message);
				case "PAYMENT_EXPIRED" -> handlePaymentExpired(message);
				default -> log.warn("알 수 없는 eventType: {}", eventType);
			}
		} catch (Exception e) {
			log.error("payment.events 처리 실패. eventType={}, message={}", eventType, message, e);
			// RuntimeException으로 전파해야 Spring Kafka가 재시도/DLQ 처리를 수행함
			throw new RuntimeException("payment.events 이벤트 처리 실패: " + eventType, e);
		}
	}

	private void handlePaymentCompleted(String message) throws Exception {
		PaymentCompletedEvent event = objectMapper.readValue(message, PaymentCompletedEvent.class);
		orderUseCase.completePayment(event);
		log.info("PAYMENT_COMPLETED 처리 완료. orderId={}", event.orderId());
	}

	private void handlePaymentFailed(String message) throws Exception {
		PaymentFailedEvent event = objectMapper.readValue(message, PaymentFailedEvent.class);
		orderUseCase.failPayment(event.eventId(), event.orderId(), event.depositAmount());
		log.info("PAYMENT_FAILED 처리 완료. orderId={}", event.orderId());
	}

	private void handlePaymentExpired(String message) throws Exception {
		PaymentExpiredEvent event = objectMapper.readValue(message, PaymentExpiredEvent.class);
		orderUseCase.expireOrder(event.eventId(), event.orderId(), event.depositAmount());
		log.info("PAYMENT_EXPIRED 처리 완료. orderId={}", event.orderId());
	}
}
