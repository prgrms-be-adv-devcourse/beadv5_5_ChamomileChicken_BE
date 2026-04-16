package jabaclass.order.infrastructure.kafka.payment;

import java.nio.charset.StandardCharsets;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import jabaclass.order.application.port.internal.OrderUseCase;
import jabaclass.order.domain.model.PaymentResultStatus;
import jabaclass.order.infrastructure.kafka.payment.dto.PaymentCompletedEvent;
import jabaclass.order.infrastructure.kafka.payment.dto.PaymentExpiredEvent;
import jabaclass.order.infrastructure.kafka.payment.dto.PaymentFailedEvent;
import jabaclass.order.infrastructure.kafka.payment.dto.PaymentRefundCompletedEvent;
import jabaclass.order.presentation.dto.request.UpdateOrderPaymentStatusRequestDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentEventsConsumer {

	private final OrderUseCase orderUseCase;
	private final ObjectMapper objectMapper;

	@KafkaListener(topics = "payment.events", groupId = "order-service")
	public void consume(ConsumerRecord<String, String> record) {
		String eventType = new String(record.headers().lastHeader("eventType").value(), StandardCharsets.UTF_8);
		String message = record.value();

		try {
			switch (eventType) {
				case "PAYMENT_COMPLETED" -> handlePaymentCompleted(message);
				case "PAYMENT_FAILED" -> handlePaymentFailed(message);
				case "PAYMENT_EXPIRED" -> handlePaymentExpired(message);
				case "PAYMENT_REFUNDED" -> handlePaymentRefunded(message);
				default -> log.warn("알 수 없는 eventType: {}", eventType);
			}
		} catch (Exception e) {
			log.error("payment.events 처리 실패. eventType={}, message={}", eventType, message, e);
			throw new RuntimeException("payment.events 이벤트 처리 실패: " + eventType, e);
		}
	}

	private void handlePaymentCompleted(String message) throws Exception {
		PaymentCompletedEvent event = objectMapper.readValue(message, PaymentCompletedEvent.class);
		orderUseCase.updatePaymentStatus(
			event.eventId(),
			event.orderId(),
			new UpdateOrderPaymentStatusRequestDto(PaymentResultStatus.SUCCESS, null)
		);
		log.info("PAYMENT_COMPLETED 처리 완료. orderId={}", event.orderId());
	}

	private void handlePaymentFailed(String message) throws Exception {
		PaymentFailedEvent event = objectMapper.readValue(message, PaymentFailedEvent.class);
		orderUseCase.updatePaymentStatus(
			event.eventId(),
			event.orderId(),
			new UpdateOrderPaymentStatusRequestDto(PaymentResultStatus.FAILED, event.depositAmount())
		);
		log.info("PAYMENT_FAILED 처리 완료. orderId={}", event.orderId());
	}

	private void handlePaymentExpired(String message) throws Exception {
		PaymentExpiredEvent event = objectMapper.readValue(message, PaymentExpiredEvent.class);
		orderUseCase.expireOrder(event.eventId(), event.orderId(), event.depositAmount());
		log.info("PAYMENT_EXPIRED 처리 완료. orderId={}", event.orderId());
	}

	private void handlePaymentRefunded(String message) throws Exception {
		PaymentRefundCompletedEvent event = objectMapper.readValue(message, PaymentRefundCompletedEvent.class);
		log.info("PAYMENT_REFUNDED 수신. orderId={} (환불 미구현)", event.orderId());
		// orderUseCase.refund(event.orderId());
	}
}
