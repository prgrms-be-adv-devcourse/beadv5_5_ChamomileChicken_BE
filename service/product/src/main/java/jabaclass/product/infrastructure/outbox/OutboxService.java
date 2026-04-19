package jabaclass.product.infrastructure.outbox;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OutboxService {

	private final OutboxRepository outboxRepository;

	@Transactional
	public void markSending(List<OutboxEvent> events) {
		for (OutboxEvent event : events) {
			if (!event.isRetryExceeded()) {
				event.markSending();
			}
		}
		outboxRepository.saveAll(events);
	}

	@Transactional
	public void markPublished(OutboxEvent event) {
		event.markPublished();
		outboxRepository.save(event);
	}

	@Transactional
	public void markFailed(OutboxEvent event) {
		event.markFailed();
		outboxRepository.save(event);
	}

	@Transactional
	public void retry(OutboxEvent event) {
		event.increaseRetry();
		event.markPending();
		outboxRepository.save(event);
	}
}
