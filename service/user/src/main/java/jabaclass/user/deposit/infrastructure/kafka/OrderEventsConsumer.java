package jabaclass.user.deposit.infrastructure.kafka;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import jabaclass.user.deposit.application.usecase.DepositUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventsConsumer {

	private final DepositUseCase depositUseCase;
	private final ObjectMapper objectMapper;

	@KafkaListener(topics = "order.events", groupId = "deposit-service")
	public void consume(ConsumerRecord<String, String> record) {
		String eventType = new String(record.headers().lastHeader("eventType").value(), StandardCharsets.UTF_8);
		String message = record.value();

		try {
			switch (eventType) {
				case "ORDER_DEPOSIT_REFUND_REQUESTED" -> handleDepositRefundRequested(message);
				case "ORDER_EXPIRED" -> handleOrderExpired(message);
				default -> log.warn("알 수 없는 eventType: {}", eventType);
			}
		} catch (Exception e) {
			log.error("order.events 처리 실패. eventType={}, message={}", eventType, message, e);
			throw new RuntimeException("order.events 이벤트 처리 실패: " + eventType, e);
		}
	}

	private void handleDepositRefundRequested(String message) throws Exception {
		OrderDepositEvent event = objectMapper.readValue(message, OrderDepositEvent.class);
		log.info("ORDER_DEPOSIT_REFUND_REQUESTED 수신 - orderId: {}, userId: {}, amount: {}",
			event.orderId(), event.userId(), event.depositAmount());
		depositUseCase.refund(event.eventId(), event.userId(), event.depositAmount(), event.orderId());
	}

	private void handleOrderExpired(String message) throws Exception {
		OrderDepositEvent event = objectMapper.readValue(message, OrderDepositEvent.class);
		log.info("ORDER_EXPIRED 수신 - orderId: {}, userId: {}, amount: {}",
			event.orderId(), event.userId(), event.depositAmount());
		depositUseCase.refund(event.eventId(), event.userId(), event.depositAmount(), event.orderId());
	}

	record OrderDepositEvent(UUID eventId, UUID orderId, UUID userId, BigDecimal depositAmount) {}
}
