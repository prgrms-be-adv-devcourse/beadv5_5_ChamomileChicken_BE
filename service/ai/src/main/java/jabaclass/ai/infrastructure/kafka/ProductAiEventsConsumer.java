package jabaclass.ai.infrastructure.kafka;

import java.nio.charset.StandardCharsets;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import jabaclass.ai.application.service.ProductEmbeddingSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class ProductAiEventsConsumer {

	private final ProductEmbeddingSyncService productEmbeddingSyncService;
	private final ObjectMapper objectMapper;

	@KafkaListener(
		topics = "product.events",
		groupId = "ai-product-indexer"
	)
	public void consume(ConsumerRecord<String, String> record) {
		String eventType = new String(record.headers().lastHeader("eventType").value(), StandardCharsets.UTF_8);
		String message = record.value();

		try {
			switch (eventType) {
				case "PRODUCT_AI_SYNCED" -> {
					ProductAiSyncedEvent event = objectMapper.readValue(message, ProductAiSyncedEvent.class);
					productEmbeddingSyncService.saveOrUpdate(event);
					log.debug("AI 임베딩 저장 완료: {}", event.productId());
				}
				case "PRODUCT_DELETED" -> {
					ProductDeletedEvent event = objectMapper.readValue(message, ProductDeletedEvent.class);
					productEmbeddingSyncService.delete(event.productId());
					log.debug("AI 임베딩 삭제 완료: {}", event.productId());
				}
				default -> log.warn("알 수 없는 eventType: {}", eventType);
			}
		} catch (Exception e) {
			throw new RuntimeException("product.events 이벤트 처리 실패: " + eventType, e);
		}
	}
}
