package jabaclass.user.deposit.application.usecase;

import java.math.BigDecimal;
import java.util.UUID;

import jabaclass.user.deposit.presentation.dto.response.DepositDetailResponseDto;
import jabaclass.user.deposit.presentation.dto.response.DepositHistoryResponseDto;
import jabaclass.user.deposit.presentation.dto.response.DepositMeResponseDto;

public interface DepositUseCase {

	void increase(UUID userId, BigDecimal amount, UUID paymentId);

	DepositMeResponseDto findMyDeposit(UUID userId);

	DepositHistoryResponseDto findAllDepositHistories(UUID userId);

	DepositDetailResponseDto findDepositHistory(UUID depositHistoryId);

	void refund(UUID eventId, UUID userId, BigDecimal amount, UUID orderId);

	void use(UUID userId, BigDecimal amount);

	boolean validate(UUID userId, BigDecimal depositAmount);
}
