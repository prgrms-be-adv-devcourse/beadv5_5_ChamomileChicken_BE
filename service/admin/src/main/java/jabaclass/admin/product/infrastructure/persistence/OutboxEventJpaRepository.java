package jabaclass.admin.product.infrastructure.persistence;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import jabaclass.admin.product.domain.model.OutboxEvent;

public interface OutboxEventJpaRepository extends JpaRepository<OutboxEvent, UUID> {

	@Query(value = """
		SELECT * FROM admin_outbox_events
		WHERE status = 'PENDING'
		   OR (status = 'SENDING' AND last_attempt_at < :threshold)
		ORDER BY created_at ASC
		LIMIT :limit
		FOR UPDATE SKIP LOCKED
	""", nativeQuery = true)
	List<OutboxEvent> findProcessableEvents(LocalDateTime threshold, int limit);
}