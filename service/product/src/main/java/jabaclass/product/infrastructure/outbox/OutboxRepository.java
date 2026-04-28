package jabaclass.product.infrastructure.outbox;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface OutboxRepository extends JpaRepository<OutboxEvent, UUID> {

	@Query(value = """
		SELECT * FROM product_outbox_events
		WHERE status = 'PENDING'
		   OR (status = 'SENDING' AND last_attempt_at < :threshold)
		ORDER BY reg_dt ASC
		LIMIT :limit
		FOR UPDATE SKIP LOCKED
	""", nativeQuery = true)
	List<OutboxEvent> findProcessableEvents(LocalDateTime threshold, int limit);

	@Query(value = """
		SELECT * FROM product_outbox_events
		WHERE status = 'FAILED'
		  AND event_type IN ('ES_SAVE', 'ES_DELETE')
	""", nativeQuery = true)
	List<OutboxEvent> findFailedEsEvents();
}
