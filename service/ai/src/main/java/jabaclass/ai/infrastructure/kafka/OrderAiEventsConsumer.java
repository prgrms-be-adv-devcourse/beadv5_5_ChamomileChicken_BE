package jabaclass.ai.infrastructure.kafka;

import java.nio.charset.StandardCharsets;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import jabaclass.ai.application.service.UserActivityService;
import jabaclass.ai.domain.model.ActionType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderAiEventsConsumer {

	private final UserActivityService userActivityService;
	private final ObjectMapper objectMapper;

	@KafkaListener(
		topics = "order.events",
		groupId = "ai-order-activity"
	)
	public void consume(ConsumerRecord<String, String> record) {
		Header eventTypeHeader = record.headers().lastHeader("eventType");
		if (eventTypeHeader == null) {
			log.error("order.events eventType 헤더가 누락되었습니다. record={}", record);
			return;
		}
		String eventType = new String(eventTypeHeader.value(), StandardCharsets.UTF_8);
		String message = record.value();

		try {
			switch (eventType) {
				case "ORDER_COMPLETED" -> {
					OrderCompletedEvent event = objectMapper.readValue(message, OrderCompletedEvent.class);
					log.info("주문 완료 이벤트 수신: userId={}, productId={}, orderId={}",
						event.userId(), event.productId(), event.orderId());
					userActivityService.recordActivity(event.userId(), event.productId(), ActionType.ORDER);
					log.info("사용자 주문 기록 저장 완료: userId={}, productId={}, orderId={}",
						event.userId(), event.productId(), event.orderId());
				}
				default -> log.debug("추천 적재 대상이 아닌 order eventType: {}", eventType);
			}
		} catch (Exception e) {
			throw new RuntimeException("order.events 이벤트 처리 실패: " + eventType, e);
		}
	}
}
