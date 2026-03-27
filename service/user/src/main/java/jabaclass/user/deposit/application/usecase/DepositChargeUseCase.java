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
	private final DepositHistoryStatusUpdater depositHistoryStatusUpdater;

	/*// POST /deposits
	@Transactional
	public DepositChargeResponseDto charge(UUID userId, DepositChargeRequestDto request) {
		User user = userRepository.findById(userId)
			.orElseThrow(() -> new BusinessException(DepositErrorCode.NOT_FOUND_USER));

		// 1. PENDING 상태로 이력 먼저 저장
		DepositHistory history = DepositHistory.of(user, null, request.chargeAmount(), DepositType.CHARGE);
		depositHistoryRepository.save(history);

		try {
			UUID paymentId = paymentClient.createPayment(userId, request.chargeAmount(), request.paymentMethod());
			history.updateStatus(DepositStatus.COMPLETED);
			history.updatePaymentId(paymentId);
			user.chargeDeposit(request.chargeAmount());

			log.info("예치금 충전 완료 userId={}, amount={}", userId, request.chargeAmount());
			return new DepositChargeResponseDto(paymentId, request.chargeAmount(), user.getDeposit(), true);

		} catch (Exception e) {
			depositHistoryStatusUpdater.updateFailed(history); // 별도 트랜잭션으로 FAILED 저장
			log.error("예치금 충전 실패 userId={}, amount={}", userId, request.chargeAmount(), e);
			throw new BusinessException(DepositErrorCode.PAYMENT_FAILED);
		}
	}*/

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
