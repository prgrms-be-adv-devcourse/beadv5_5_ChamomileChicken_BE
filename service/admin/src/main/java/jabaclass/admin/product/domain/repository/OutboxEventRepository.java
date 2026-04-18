package jabaclass.admin.product.domain.repository;

import java.time.LocalDateTime;
import java.util.List;

import jabaclass.admin.product.domain.model.OutboxEvent;

public interface OutboxEventRepository {
	OutboxEvent save(OutboxEvent event);
	List<OutboxEvent> saveAll(List<OutboxEvent> events);
	List<OutboxEvent> findProcessableEvents(LocalDateTime threshold, int limit);
}