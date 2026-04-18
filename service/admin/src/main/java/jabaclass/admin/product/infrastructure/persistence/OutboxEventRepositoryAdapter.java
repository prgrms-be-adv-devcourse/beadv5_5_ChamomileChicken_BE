package jabaclass.admin.product.infrastructure.persistence;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Repository;

import jabaclass.admin.product.domain.model.OutboxEvent;
import jabaclass.admin.product.domain.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class OutboxEventRepositoryAdapter implements OutboxEventRepository {

	private final OutboxEventJpaRepository outboxEventJpaRepository;

	@Override
	public OutboxEvent save(OutboxEvent event) {
		return outboxEventJpaRepository.save(event);
	}

	@Override
	public List<OutboxEvent> saveAll(List<OutboxEvent> events) {
		return outboxEventJpaRepository.saveAll(events);
	}

	@Override
	public List<OutboxEvent> findProcessableEvents(LocalDateTime threshold, int limit) {
		return outboxEventJpaRepository.findProcessableEvents(threshold, limit);
	}
}