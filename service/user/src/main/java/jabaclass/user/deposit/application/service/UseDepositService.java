package jabaclass.user.deposit.application.service;

import java.math.BigDecimal;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jabaclass.user.deposit.domain.DepositHistory;
import jabaclass.user.deposit.domain.DepositStatus;
import jabaclass.user.deposit.domain.DepositType;
import jabaclass.user.deposit.domain.exception.DepositErrorCode;
import jabaclass.user.deposit.domain.exception.DepositException;
import jabaclass.user.deposit.domain.repository.DepositHistoryRepository;
import jabaclass.user.user.domain.model.User;
import jabaclass.user.user.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UseDepositService {

	private final UserRepository userRepository;
	private final DepositHistoryRepository depositHistoryRepository;

	@Transactional
	public void use(UUID userId, BigDecimal amount) {
		User user = userRepository.findByIdWithLock(userId)
			.orElseThrow(() -> new DepositException(DepositErrorCode.NOT_FOUND_USER));

		user.deductDeposit(amount);

		DepositHistory history = DepositHistory.of(user, null, amount, DepositType.PAYMENT);
		history.updateStatus(DepositStatus.COMPLETED);
		depositHistoryRepository.save(history);
	}
}