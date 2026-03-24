package jabaclass.user.deposit.application.usecase;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jabaclass.user.common.error.BusinessException;
import jabaclass.user.deposit.domain.DepositHistory;
import jabaclass.user.deposit.domain.DepositStatus;
import jabaclass.user.deposit.domain.DepositType;
import jabaclass.user.deposit.domain.error.DepositErrorCode;
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
	private final PaymentClient paymentClient;

	// POST /deposits
	@Transactional
	public DepositChargeResponseDto charge(UUID userId, DepositChargeRequestDto request) {
		User user = userRepository.findById(userId)
			.orElseThrow(() -> new BusinessException(DepositErrorCode.NOT_FOUND_USER));

		// 1. PENDING 상태로 이력 먼저 저장
		DepositHistory history = DepositHistory.of(user, null, request.chargeAmount(), DepositType.CHARGE);
		depositHistoryRepository.save(history);

		try {
			// 2. 결제 시도
			UUID paymentId = paymentClient.createPayment(userId, request.chargeAmount(), request.paymentMethod());

			// 3. 결제 성공 시 COMPLETED + 예치금 충전
			history.updateStatus(DepositStatus.COMPLETED);
			history.updatePaymentId(paymentId);
			user.chargeDeposit(request.chargeAmount());

			log.info("예치금 충전 완료 userId={}, amount={}", userId, request.chargeAmount());
			return new DepositChargeResponseDto(paymentId, request.chargeAmount(), user.getDeposit(), true);

		} catch (Exception e) {
			// 4. 결제 실패 시 FAILED
			history.updateStatus(DepositStatus.FAILED);
			log.error("예치금 충전 실패 userId={}, amount={}", userId, request.chargeAmount(), e);
			throw new IllegalStateException("결제에 실패했습니다.");
		}
	}
}
