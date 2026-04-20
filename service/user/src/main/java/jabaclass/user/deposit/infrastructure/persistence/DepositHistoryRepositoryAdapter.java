package jabaclass.user.deposit.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import jabaclass.user.deposit.domain.DepositHistory;
import jabaclass.user.deposit.domain.repository.DepositHistoryRepository;
import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class DepositHistoryRepositoryAdapter implements DepositHistoryRepository {

	private final DepositHistoryJpaRepository depositHistoryJpaRepository;

	@Override
	public DepositHistory save(DepositHistory depositHistory) {
		return depositHistoryJpaRepository.save(depositHistory);
	}

	@Override
	public Optional<DepositHistory> findById(UUID id) {
		return depositHistoryJpaRepository.findById(id);
	}

	@Override
	public List<DepositHistory> findAllByUserId(UUID userId) {
		return depositHistoryJpaRepository.findAllByUserId(userId);
	}
}
