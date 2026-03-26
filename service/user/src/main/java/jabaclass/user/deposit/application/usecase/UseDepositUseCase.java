package jabaclass.user.deposit.application.usecase;

import java.math.BigDecimal;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jabaclass.user.deposit.domain.exception.DepositErrorCode;
import jabaclass.user.deposit.domain.exception.DepositException;
import jabaclass.user.user.domain.model.User;
import jabaclass.user.user.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UseDepositUseCase {

	private final UserRepository userRepository;

	@Transactional
	public void use(UUID userId, BigDecimal amount) {
		User user = userRepository.findByIdWithLock(userId)
			.orElseThrow(() -> new DepositException(DepositErrorCode.NOT_FOUND_USER));

		user.deductDeposit(amount);
	}
}
