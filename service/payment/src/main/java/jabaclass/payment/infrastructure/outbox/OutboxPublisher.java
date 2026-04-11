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

	@Scheduled(fixedDelay = 1000) // 1초마다 실행
	@Transactional
	public void publish() {

		List<OutboxEvent> events = outboxRepository.findByStatus(OutboxStatus.PENDING); // PENDING 상태의 이벤트를 조회하여 Kafka로 전송

		for (OutboxEvent event : events) {
			try {

				if (event.isRetryExceeded()) {
					event.markFailed(); // retry 횟수 초과 시 FAILED(DLQ) 처리
					continue;
				}

				String topic = event.getEventType().getTopic();

				kafkaTemplate.send(topic, event.getPayload());

				event.markPublished(); // 성공 시 PUBLISHED 상태로 변경

			} catch (Exception e) {
				event.increaseRetry(); // 실패 시 retryCount 증가
			}
		}
	}
}