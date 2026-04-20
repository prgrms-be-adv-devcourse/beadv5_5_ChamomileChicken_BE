package jabaclass.user.deposit.domain.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import jabaclass.user.deposit.domain.DepositHistory;

public interface DepositHistoryRepository {

	DepositHistory save(DepositHistory depositHistory);

	Optional<DepositHistory> findById(UUID id);

	List<DepositHistory> findAllByUserId(UUID userId);
}
