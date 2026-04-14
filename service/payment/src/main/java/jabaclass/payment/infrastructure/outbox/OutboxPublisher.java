package jabaclass.payment.infrastructure.outbox;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class OutboxPublisher {

	private final OutboxRepository outboxRepository;
	private final OutboxService outboxService;
	private final KafkaTemplate<String, String> kafkaTemplate;

	@Scheduled(fixedDelay = 1000) // 1초마다 Outbox 이벤트 발행 시도
	public void publish() {

		// SENDING 상태에서 멈춘 이벤트 찾기
		LocalDateTime threshold = LocalDateTime.now().minusMinutes(5); // 5분 기준
		List<OutboxEvent> events =
			outboxRepository.findProcessableEvents(threshold, PageRequest.of(0, 100));

		// [트랜잭션 1] PENDING → SENDING  (commit)
		outboxService.markSending(events);

		// [트랜잭션 없음] Kafka 전송
		for (OutboxEvent event : events) {

			// retry 횟수 초과 시 → DLQ(FAILED)
			if (event.isRetryExceeded()) {
				// [트랜잭션 2] PENDING/SENDING → FAILED (commit)
				outboxService.markFailed(event);
				continue;
			}

			try {
				kafkaTemplate.send(
					event.getEventType().getTopic(),
					event.getAggregateId(), // 같은 aggregate는 같은 partition → 순서 보장
					event.getPayload()
				).get(); // send().get()을 통해 Kafka가 메시지를 정상 수신했는지 확인

				// [트랜잭션 2] SENDING → PUBLISHED (commit)
				outboxService.markPublished(event);


			} catch (Exception e) { // Kafka 전송 실패 시 재시도 → 다음 스케줄에서 재처리
				outboxService.retry(event);
			}
		}
	}
}
