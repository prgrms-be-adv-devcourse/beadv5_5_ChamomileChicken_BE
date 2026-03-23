package jabaclass.user.deposit.application.usecase;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jabaclass.user.deposit.domain.DepositHistory;
import jabaclass.user.deposit.domain.DepositType;
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
			.orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));

		// prepareId -> paymentId 받아옴
		UUID paymentId = paymentClient.createPayment(userId, request.chargeAmount(), request.paymentMethod());
		// 결제 승인에 대한 예치금 증가 로직
		user.chargeDeposit(request.chargeAmount());

		// 예치금 히스토리 저장
		DepositHistory history = DepositHistory.of(user, paymentId, request.chargeAmount(), DepositType.CHARGE);
		depositHistoryRepository.save(history);

		log.info("예치금 충전 완료 userId={}, amount={}", userId, request.chargeAmount());

		return new DepositChargeResponseDto(paymentId, request.chargeAmount(), user.getDeposit(), true);
	}
}
