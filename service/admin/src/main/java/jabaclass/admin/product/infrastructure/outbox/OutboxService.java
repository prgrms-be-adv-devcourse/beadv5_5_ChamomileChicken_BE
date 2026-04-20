package jabaclass.admin.product.infrastructure.outbox;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jabaclass.admin.product.domain.model.OutboxEvent;
import jabaclass.admin.product.domain.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OutboxService {

	private final OutboxEventRepository outboxEventRepository;

	@Transactional(transactionManager = "productTransactionManager")
	public List<OutboxEvent> findAndMarkSending(LocalDateTime threshold, int limit) {
		List<OutboxEvent> events = outboxEventRepository.findProcessableEvents(threshold, limit);
		for (OutboxEvent event : events) {
			if (!event.isRetryExceeded()) {
				event.markSending();
			}
		}
		outboxEventRepository.saveAll(events);
		return events;
	}

	@Transactional(transactionManager = "productTransactionManager")
	public void markPublished(OutboxEvent event) {
		event.markPublished();
		outboxEventRepository.save(event);
	}

	@Transactional(transactionManager = "productTransactionManager")
	public void markFailed(OutboxEvent event) {
		event.markFailed();
		outboxEventRepository.save(event);
	}

	@Transactional(transactionManager = "productTransactionManager")
	public void retry(OutboxEvent event) {
		event.increaseRetry();
		event.markPending();
		outboxEventRepository.save(event);
	}
}
