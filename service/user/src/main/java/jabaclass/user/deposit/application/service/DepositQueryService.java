package jabaclass.user.deposit.application.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jabaclass.user.common.error.BusinessException;
import jabaclass.user.deposit.domain.DepositHistory;
import jabaclass.user.deposit.domain.exception.DepositErrorCode;
import jabaclass.user.deposit.domain.repository.DepositHistoryRepository;
import jabaclass.user.deposit.presentation.dto.response.DepositDetailResponseDto;
import jabaclass.user.deposit.presentation.dto.response.DepositHistoryItemDto;
import jabaclass.user.deposit.presentation.dto.response.DepositHistoryResponseDto;
import jabaclass.user.deposit.presentation.dto.response.DepositMeResponseDto;
import jabaclass.user.user.domain.model.User;
import jabaclass.user.user.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DepositQueryService {

	private final UserRepository userRepository;
	private final DepositHistoryRepository depositHistoryRepository;

	public DepositMeResponseDto findMyDeposit(UUID userId) {
		User user = userRepository.findById(userId)
			.orElseThrow(() -> new BusinessException(DepositErrorCode.NOT_FOUND_USER));

		return new DepositMeResponseDto(user.getId(), user.getDeposit());
	}

	public DepositHistoryResponseDto findAllDepositHistories(UUID userId) {
		List<DepositHistoryItemDto> items = depositHistoryRepository.findAllByUserId(userId)
			.stream()
			.map(history -> new DepositHistoryItemDto(
				history.getId(),
				history.getUser().getId(),
				history.getPaymentId(),
				history.getType(),
				history.getAmount()
			))
			.toList();

		return new DepositHistoryResponseDto(items);
	}

	public DepositDetailResponseDto findDepositHistory(UUID depositHistoryId) {
		DepositHistory history = depositHistoryRepository.findById(depositHistoryId)
			.orElseThrow(() -> new BusinessException(DepositErrorCode.NOT_FOUND_DEPOSIT_HISTORY));

		return new DepositDetailResponseDto(
			history.getId(),
			history.getUser().getId(),
			history.getPaymentId(),
			history.getType(),
			history.getAmount()
		);
	}
}