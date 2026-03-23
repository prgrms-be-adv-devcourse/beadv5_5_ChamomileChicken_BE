package jabaclass.user.deposit.application.usecase;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jabaclass.user.deposit.domain.DepositHistory;
import jabaclass.user.deposit.domain.repository.DepositHistoryRepository;
import jabaclass.user.deposit.presentation.dto.response.DepositDetailResponseDto;
import jabaclass.user.deposit.presentation.dto.response.DepositHistoryItemDto;
import jabaclass.user.deposit.presentation.dto.response.DepositHistoryResponseDto;
import jabaclass.user.deposit.presentation.dto.response.DepositMeResponseDto;
import jabaclass.user.user.domain.model.User;
import jabaclass.user.user.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DepositQueryUseCase {

	private final UserRepository userRepository;
	private final DepositHistoryRepository depositHistoryRepository;

	// 예치금 조회
	// GET /deposits/me
	public DepositMeResponseDto findMyDeposit(UUID userId) {
		User user = userRepository.findById(userId)
			.orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));

		return new DepositMeResponseDto(user.getId(), user.getDeposit());
	}

	// 예치금 이력 조회
	// GET /deposists
	public DepositHistoryResponseDto findAllDepositHistories(UUID userId) {
		List<DepositHistoryItemDto> items = depositHistoryRepository.findAllByUserId(userId)
			.stream()
			.map(history -> new DepositHistoryItemDto(
				history.getId(),
				history.getUser().getId(),
				history.getPaymentId(),    // 누락된 paymentId 추가
				history.getType(),
				history.getAmount()
			))
			.toList();

		return new DepositHistoryResponseDto(items);
	}

	// 예치금 상세 조회
	// GET /deposits/{예치금Id}
	public DepositDetailResponseDto findDepositHistory(UUID depositHistoryId) {
		DepositHistory history = depositHistoryRepository.findById(depositHistoryId)
			.orElseThrow(() -> new IllegalArgumentException("존재하지 않는 예치금 이력입니다."));

		return new DepositDetailResponseDto(
			history.getId(),
			history.getUser().getId(),
			history.getPaymentId(),
			history.getType(),
			history.getAmount()
		);
	}
}
