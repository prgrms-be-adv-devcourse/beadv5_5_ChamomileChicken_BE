package jabaclass.product.infrastructure.kafka;

import java.nio.charset.StandardCharsets;

import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.fasterxml.jackson.databind.ObjectMapper;

import jabaclass.product.infrastructure.event.dto.ProductAiSyncedEvent;
import jabaclass.product.infrastructure.event.dto.ProductDeletedEvent;
import jabaclass.product.infrastructure.event.dto.ProductViewedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class ProductAiEventsPublisher {

	private final KafkaTemplate<String, String> kafkaTemplate;
	private final ObjectMapper objectMapper;

	@Async
	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void handle(ProductAiSyncedEvent event) {
		try {
			send(ProductEventType.PRODUCT_AI_SYNCED, event.productId().toString(), objectMapper.writeValueAsString(event));
		} catch (Exception e) {
			log.error("AI 동기화 이벤트 발행 실패: productId={}, error={}",
				event.productId(), e.getMessage(), e);
		}
	}

	@Async
	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void handle(ProductDeletedEvent event) {
		try {
			send(ProductEventType.PRODUCT_DELETED, event.productId().toString(), objectMapper.writeValueAsString(event));
		} catch (Exception e) {
			log.error("상품 삭제 이벤트 발행 실패: productId={}, error={}",
				event.productId(), e.getMessage(), e);
		}
	}

	@Async
	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void handle(ProductViewedEvent event) {
		try {
			send(ProductEventType.PRODUCT_VIEWED, event.userId().toString(), objectMapper.writeValueAsString(event));
		} catch (Exception e) {
			log.error("상품 조회 이벤트 발행 실패: userId={}, productId={}, error={}",
				event.userId(), event.productId(), e.getMessage(), e);
		}
	}

	private void send(ProductEventType eventType, String key, String payload) {
		ProducerRecord<String, String> record = new ProducerRecord<>(
			eventType.getTopic(),
			key,
			payload
		);
		record.headers().add(
			"eventType",
			eventType.name().getBytes(StandardCharsets.UTF_8)
		);
		kafkaTemplate.send(record);
	}
}
