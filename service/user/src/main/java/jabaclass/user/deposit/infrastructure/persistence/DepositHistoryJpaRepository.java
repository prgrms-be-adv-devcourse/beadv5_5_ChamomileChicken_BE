package jabaclass.user.deposit.infrastructure.persistence;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import jabaclass.user.deposit.domain.DepositHistory;

public interface DepositHistoryRepository extends JpaRepository<DepositHistory, UUID> {
	List<DepositHistory> findAllByUserId(UUID userId);
}
