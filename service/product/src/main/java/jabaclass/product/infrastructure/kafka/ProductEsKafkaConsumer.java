package jabaclass.product.infrastructure.kafka;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import jabaclass.product.config.KafkaTopicConfig;
import jabaclass.product.domain.repository.ProductSearchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class ProductEsKafkaConsumer {

	private final ProductSearchRepository productSearchRepository;
	private final ObjectMapper objectMapper;

	@KafkaListener(
		topics = KafkaTopicConfig.PRODUCT_ES_TOPIC,
		groupId = "product-es-indexer"
	)
	public void consume(String message) {
		try {
			ProductEsIndexMessage event = objectMapper.readValue(message, ProductEsIndexMessage.class);
			if ("SAVE".equals(event.operation())) {
				productSearchRepository.save(event.document());
				log.debug("ES 색인 완료: {}", event.document().getId());
			} else if ("DELETE".equals(event.operation())) {
				productSearchRepository.deleteById(event.productId());
				log.debug("ES 삭제 완료: {}", event.productId());
			} else {
				log.warn("알 수 없는 ES 색인 operation: {}", event.operation());
			}
		} catch (Exception e) {
			throw new RuntimeException("ES 색인 이벤트 처리 실패: " + e.getMessage(), e);
		}
	}
}
