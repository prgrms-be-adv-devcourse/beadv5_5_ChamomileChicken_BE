package jabaclass.settlement.infrastructure.kafka;

import java.nio.charset.StandardCharsets;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import jabaclass.settlement.infrastructure.kafka.dto.SellerApprovedEvent;
import jabaclass.settlement.infrastructure.kafka.dto.SettlementPaymentCompletedEvent;
import jabaclass.settlement.infrastructure.kafka.dto.SettlementRefundCompletedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class SettlementEventsConsumer {

	private final SettlementTargetEventHandler settlementTargetEventHandler;
	private final SellerPromotionEventHandler sellerPromotionEventHandler;
	private final ObjectMapper objectMapper;

	@KafkaListener(topics = "settlement.events", groupId = "settlement-service")
	public void consume(ConsumerRecord<String, String> record) {
		Header eventTypeHeader = record.headers().lastHeader("eventType");
		if (eventTypeHeader == null) {
			log.warn("settlement.events 헤더 누락. key={}, message={}", record.key(), record.value());
			return;
		}
		String eventType = new String(eventTypeHeader.value(), StandardCharsets.UTF_8);
		String message = record.value();

		try {
			switch (eventType) {
				case "SETTLEMENT_PAYMENT_COMPLETED" -> {
					SettlementPaymentCompletedEvent event = objectMapper.readValue(
						message,
						SettlementPaymentCompletedEvent.class
					);
					settlementTargetEventHandler.handlePaymentCompleted(event);
				}
				case "SETTLEMENT_REFUND_COMPLETED" -> {
					SettlementRefundCompletedEvent event = objectMapper.readValue(
						message,
						SettlementRefundCompletedEvent.class
					);
					settlementTargetEventHandler.handleRefundCompleted(event);
				}
				case "USER_SELLER_APPROVED" -> {
					SellerApprovedEvent event = objectMapper.readValue(
						message,
						SellerApprovedEvent.class
					);
					sellerPromotionEventHandler.handleSellerApproved(event);
				}
				default -> log.warn("알 수 없는 settlement eventType: {}", eventType);
			}
		} catch (Exception e) {
			log.error("settlement.events 처리 실패. eventType={}, message={}", eventType, message, e);
			throw new RuntimeException("settlement.events 이벤트 처리 실패: " + eventType, e);
		}
	}
}
