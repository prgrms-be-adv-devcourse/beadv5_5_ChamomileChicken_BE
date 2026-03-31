package jabaclass.user.deposit.application.service;

import java.math.BigDecimal;
import java.util.UUID;

import org.springframework.stereotype.Service;

import jabaclass.user.deposit.application.usecase.DepositUseCase;
import jabaclass.user.deposit.presentation.dto.response.DepositDetailResponseDto;
import jabaclass.user.deposit.presentation.dto.response.DepositHistoryResponseDto;
import jabaclass.user.deposit.presentation.dto.response.DepositMeResponseDto;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DepositService implements DepositUseCase {

	private final DepositChargeService depositChargeService;
	private final DepositQueryService depositQueryService;
	private final RefundDepositService refundDepositService;
	private final UseDepositService useDepositService;
	private final ValidateDepositService validateDepositService;

	@Override
	public void increase(UUID userId, BigDecimal amount, UUID paymentId) {
		depositChargeService.increase(userId, amount, paymentId);
	}

	@Override
	public DepositMeResponseDto findMyDeposit(UUID userId) {
		return depositQueryService.findMyDeposit(userId);
	}

	@Override
	public DepositHistoryResponseDto findAllDepositHistories(UUID userId) {
		return depositQueryService.findAllDepositHistories(userId);
	}

	@Override
	public DepositDetailResponseDto findDepositHistory(UUID depositHistoryId) {
		return depositQueryService.findDepositHistory(depositHistoryId);
	}

	@Override
	public void refund(UUID userId, BigDecimal amount, UUID paymentId) {
		refundDepositService.refund(userId, amount, paymentId);
	}

	@Override
	public void use(UUID userId, BigDecimal amount) {
		useDepositService.use(userId, amount);
	}

	@Override
	public boolean validate(UUID userId, BigDecimal depositAmount) {
		return validateDepositService.validate(userId, depositAmount);
	}
}