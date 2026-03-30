package jabaclass.user.deposit.application.service;

import java.math.BigDecimal;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jabaclass.user.deposit.domain.exception.DepositErrorCode;
import jabaclass.user.deposit.domain.exception.DepositException;
import jabaclass.user.user.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UseDepositService {

	private final UserRepository userRepository;

	@Transactional
	public void use(UUID userId, BigDecimal amount) {
		userRepository.findByIdWithLock(userId)
			.orElseThrow(() -> new DepositException(DepositErrorCode.NOT_FOUND_USER))
			.deductDeposit(amount);
	}
}