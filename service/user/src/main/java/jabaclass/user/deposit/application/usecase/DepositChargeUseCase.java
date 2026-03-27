package jabaclass.user.deposit.application.usecase;

import java.math.BigDecimal;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jabaclass.user.common.error.BusinessException;
import jabaclass.user.deposit.application.DepositHistoryStatusUpdater;
import jabaclass.user.deposit.domain.DepositHistory;
import jabaclass.user.deposit.domain.DepositStatus;
import jabaclass.user.deposit.domain.DepositType;
import jabaclass.user.deposit.domain.exception.DepositErrorCode;
import jabaclass.user.deposit.domain.repository.DepositHistoryRepository;
import jabaclass.user.deposit.infrastructure.client.PaymentClient;
import jabaclass.user.deposit.presentation.dto.request.DepositChargeRequestDto;
import jabaclass.user.deposit.presentation.dto.response.DepositChargeResponseDto;
import jabaclass.user.user.domain.model.User;
import jabaclass.user.user.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DepositChargeUseCase {

	private final UserRepository userRepository;
	private final DepositHistoryRepository depositHistoryRepository;

	@Transactional
	public void increase(UUID userId, BigDecimal amount, UUID paymentId) {

		User user = userRepository.findById(userId)
			.orElseThrow(() -> new BusinessException(DepositErrorCode.NOT_FOUND_USER));

		// 1. 예치금 증가
		user.chargeDeposit(amount);

		// 2. 이력 저장 (COMPLETED 바로)
		DepositHistory history = DepositHistory.of(
			user,
			paymentId,
			amount,
			DepositType.CHARGE
		);

		history.updateStatus(DepositStatus.COMPLETED);

		depositHistoryRepository.save(history);

		log.info("예치금 증가 완료 userId={}, amount={}", userId, amount);
	}
}
