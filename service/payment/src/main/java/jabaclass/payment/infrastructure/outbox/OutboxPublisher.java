package jabaclass.payment.infrastructure.outbox;

import java.util.List;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class OutboxPublisher {

	private final OutboxRepository outboxRepository;
	private final KafkaTemplate<String, String> kafkaTemplate;

	@Scheduled(fixedDelay = 1000)
	@Transactional
	public void publish() {

		List<OutboxEvent> events = outboxRepository.findByStatus(OutboxStatus.PENDING);

		for (OutboxEvent event : events) {
			try {

				if (event.isRetryExceeded()) {
					event.markFailed(); // DLQ
					continue;
				}

				String topic = event.getEventType().getTopic();

				kafkaTemplate.send(topic, event.getPayload());

				event.markPublished();

			} catch (Exception e) {
				event.increaseRetry();
			}
		}
	}
}