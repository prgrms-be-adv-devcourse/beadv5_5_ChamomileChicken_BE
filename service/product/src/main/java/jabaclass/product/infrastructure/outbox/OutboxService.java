package jabaclass.product.infrastructure.outbox;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OutboxService {

	private final OutboxRepository outboxRepository;

	@Transactional
	public List<OutboxEvent> findAndMarkSending(LocalDateTime threshold, int limit) {
		List<OutboxEvent> events = outboxRepository.findProcessableEvents(threshold, limit);
		for (OutboxEvent event : events) {
			if (!event.isRetryExceeded()) {
				event.markSending();
			}
		}
		outboxRepository.saveAll(events);
		return events;
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
		if (event.isRetryExceeded()) {
			event.markFailed();
		} else {
			event.markPending();
		}
		outboxRepository.save(event);
	}

	@Transactional
	public int resetFailedEsEvents() {
		List<OutboxEvent> failed = outboxRepository.findFailedEsEvents();
		failed.forEach(OutboxEvent::resetForRetry);
		outboxRepository.saveAll(failed);
		return failed.size();
	}
}
