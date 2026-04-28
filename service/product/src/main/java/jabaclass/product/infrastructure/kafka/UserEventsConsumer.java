package jabaclass.product.infrastructure.kafka;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import jabaclass.product.domain.repository.ProductSearchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class UserEventsConsumer {

	private final ProductSearchRepository productSearchRepository;
	private final ObjectMapper objectMapper;

	@KafkaListener(topics = "user.events", groupId = "product-user-sync")
	public void consume(ConsumerRecord<String, String> record) {
		var eventTypeHeader = record.headers().lastHeader("eventType");
		if (eventTypeHeader == null) {
			log.warn("user.events 메시지에 eventType 헤더 없음. 무시합니다. offset={}", record.offset());
			return;
		}
		String eventType = new String(eventTypeHeader.value(), StandardCharsets.UTF_8);

		try {
			switch (eventType) {
				case "USER_NAME_CHANGED" -> handleUserNameChanged(record.value());
				default -> log.warn("알 수 없는 user.events eventType: {}", eventType);
			}
		} catch (Exception e) {
			log.error("user.events 처리 실패. eventType={}", eventType, e);
			throw new RuntimeException("user.events 이벤트 처리 실패: " + eventType, e);
		}
	}

	private void handleUserNameChanged(String message) throws Exception {
		UserNameChangedEvent event = objectMapper.readValue(message, UserNameChangedEvent.class);
		log.info("USER_NAME_CHANGED 수신 - userId={}, newName={}", event.userId(), event.newName());
		productSearchRepository.updateSellerNameForAll(event.userId().toString(), event.newName());
	}

	record UserNameChangedEvent(UUID userId, String newName) {
	}
}
