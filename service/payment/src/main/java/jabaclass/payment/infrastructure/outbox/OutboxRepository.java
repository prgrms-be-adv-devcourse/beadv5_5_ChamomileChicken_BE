package jabaclass.payment.infrastructure.outbox;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface OutboxRepository extends JpaRepository<OutboxEvent, UUID> {
	@Query("""
        SELECT e FROM OutboxEvent e
        WHERE e.status = 'PENDING'
           OR (e.status = 'SENDING' AND e.lastAttemptAt < :threshold)
	   ORDER BY e.createdAt ASC
    """)
	List<OutboxEvent> findProcessableEvents(
		LocalDateTime threshold,
		Pageable pageable
	);
}
