package jabaclass.admin.product.infrastructure.kafka;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class AdminProductEventPublisher {

	public static final String TOPIC = "admin.product";

	private final KafkaTemplate<String, String> kafkaTemplate;
	private final ObjectMapper objectMapper;

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void publish(AdminProductEvent event) {
		try {
			kafkaTemplate.send(TOPIC, event.productId(), objectMapper.writeValueAsString(event));
		} catch (Exception e) {
			log.error("[ADMIN] AdminProductEvent 발행 실패. type={}, productId={}", event.type(), event.productId(), e);
		}
	}
}
