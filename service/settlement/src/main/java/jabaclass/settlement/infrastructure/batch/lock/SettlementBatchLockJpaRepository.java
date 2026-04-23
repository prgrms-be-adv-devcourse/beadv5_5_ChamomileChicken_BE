package jabaclass.settlement.infrastructure.batch.lock;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

public interface SettlementBatchLockJpaRepository extends JpaRepository<SettlementBatchLock, UUID> {

	@Transactional
	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query("delete from SettlementBatchLock lock where lock.lockKey = :lockKey")
	void deleteByLockKey(String lockKey);

	@Transactional
	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query("delete from SettlementBatchLock lock where lock.expiresAt < :now")
	void deleteByExpiresAtBefore(LocalDateTime now);
}
