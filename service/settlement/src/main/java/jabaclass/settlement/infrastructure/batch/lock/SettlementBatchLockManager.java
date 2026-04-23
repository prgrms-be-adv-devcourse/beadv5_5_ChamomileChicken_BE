package jabaclass.settlement.infrastructure.batch.lock;

import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class SettlementBatchLockManager {

	private static final String LOCK_KEY_PREFIX = "settlement:";

	private final SettlementBatchLockJpaRepository settlementBatchLockJpaRepository;

	@Value("${settlement.batch.lock.expire-minutes:360}")
	private long expireMinutes;

	public String createMonthlyLockKey(String settlementMonth) {
		return LOCK_KEY_PREFIX + settlementMonth;
	}

	public boolean acquire(String lockKey, String jobName, String settlementMonth) {
		LocalDateTime now = LocalDateTime.now();
		if (tryAcquire(lockKey, jobName, settlementMonth, now)) {
			return true;
		}

		settlementBatchLockJpaRepository.deleteByLockKeyAndExpiresAtBefore(lockKey, now);
		return tryAcquire(lockKey, jobName, settlementMonth, now);
	}

	private boolean tryAcquire(String lockKey, String jobName, String settlementMonth, LocalDateTime now) {
		try {
			settlementBatchLockJpaRepository.saveAndFlush(new SettlementBatchLock(
				lockKey,
				jobName,
				settlementMonth,
				now.plusMinutes(expireMinutes)
			));
			return true;
		} catch (DataIntegrityViolationException e) {
			return false;
		}
	}

	public void release(String lockKey) {
		if (lockKey == null || lockKey.isBlank()) {
			return;
		}

		settlementBatchLockJpaRepository.deleteByLockKey(lockKey);
	}
}
