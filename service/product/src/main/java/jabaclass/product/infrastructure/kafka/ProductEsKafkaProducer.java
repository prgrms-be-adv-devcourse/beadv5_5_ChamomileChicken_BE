package jabaclass.product.infrastructure.kafka;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.fasterxml.jackson.databind.ObjectMapper;

import jabaclass.product.infrastructure.event.dto.ProductEsDeleteEvent;
import jabaclass.product.infrastructure.event.dto.ProductEsSaveEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class ProductEsKafkaProducer {

	public static final String TOPIC = "product.es.index";

	private final KafkaTemplate<String, String> kafkaTemplate;
	private final ObjectMapper objectMapper;

	@Async
	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void handleSave(ProductEsSaveEvent event) {
		send(ProductEsIndexMessage.save(event.document()));
	}

	@Async
	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void handleDelete(ProductEsDeleteEvent event) {
		send(ProductEsIndexMessage.delete(event.productId()));
	}

	private void send(ProductEsIndexMessage message) {
		try {
			String payload = objectMapper.writeValueAsString(message);
			kafkaTemplate.send(TOPIC, payload);
			log.debug("ES 색인 이벤트 발행: operation={}, productId={}",
				message.operation(),
				message.document() != null ? message.document().getId() : message.productId());
		} catch (Exception e) {
			log.error("ES 색인 이벤트 발행 실패: operation={}, error={}",
				message.operation(), e.getMessage(), e);
		}
	}
}
