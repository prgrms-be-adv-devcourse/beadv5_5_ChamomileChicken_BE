package jabaclass.user.deposit.application.usecase;

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
@Transactional(readOnly = true)
public class RefundDepositUseCase {

	private final UserRepository userRepository;
	private final DepositHistoryRepository depositHistoryRepository;

	@Transactional
	public void refund(UUID userId, BigDecimal amount, UUID paymentId) {
		User user = userRepository.findByIdWithLock(userId)
			.orElseThrow(() -> new DepositException(DepositErrorCode.NOT_FOUND_USER));

		user.chargeDeposit(amount);

		DepositHistory history = DepositHistory.of(
			user,
			paymentId,
			amount,
			DepositType.REFUND
		);
		history.updateStatus(DepositStatus.COMPLETED);
		depositHistoryRepository.save(history);
	}

}
