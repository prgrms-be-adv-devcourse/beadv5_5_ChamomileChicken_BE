package jabaclass.product.infrastructure.kafka.admin;

import java.util.UUID;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import jabaclass.product.domain.repository.ProductSearchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class AdminProductKafkaConsumer {

	private final AdminProductEventHandler handler;
	private final ProductSearchRepository productSearchRepository;
	private final ObjectMapper objectMapper;

	@KafkaListener(topics = "admin.product", groupId = "product-admin-consumer")
	public void consume(String message) {
		try {
			AdminProductMessage event = objectMapper.readValue(message, AdminProductMessage.class);

			if ("FORCE_DOWN".equals(event.type())) {
				handleForceDown(event);
			} else {
				log.warn("알 수 없는 admin.product type: {}", event.type());
			}
		} catch (Exception e) {
			throw new RuntimeException("admin.product 이벤트 처리 실패: " + e.getMessage(), e);
		}
	}

	private void handleForceDown(AdminProductMessage event) {
		UUID productId = UUID.fromString(event.productId());

		handler.processForceDown(productId);

		try {
			productSearchRepository.deleteById(event.productId());
			log.info("FORCE_DOWN ES 삭제 완료. productId={}", event.productId());
		} catch (Exception esEx) {
			log.error("ES 삭제 실패, 보상 트랜잭션 실행. productId={}", event.productId(), esEx);
			handler.restoreProductStatus(productId);
			throw new RuntimeException("FORCE_DOWN ES 처리 실패. productId=" + event.productId(), esEx);
		}
	}
}
