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
@Transactional(readOnly = true)
public class ValidateDepositService {

	private final UserRepository userRepository;

	public boolean validate(UUID userId, BigDecimal depositAmount) {
		return userRepository.findById(userId)
			.orElseThrow(() -> new DepositException(DepositErrorCode.NOT_FOUND_USER))
			.getDeposit().compareTo(depositAmount) >= 0;
	}
}