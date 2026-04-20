package jabaclass.user.deposit.application.service;

import java.math.BigDecimal;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jabaclass.user.deposit.domain.DepositHistory;
import jabaclass.user.deposit.domain.DepositStatus;
import jabaclass.user.deposit.domain.DepositType;
import jabaclass.user.deposit.domain.exception.DepositErrorCode;
import jabaclass.user.deposit.domain.exception.DepositException;
import jabaclass.user.deposit.domain.repository.DepositHistoryRepository;
import jabaclass.user.deposit.infrastructure.idempotency.ProcessedEvent;
import jabaclass.user.deposit.infrastructure.idempotency.ProcessedEventRepository;
import jabaclass.user.user.domain.model.User;
import jabaclass.user.user.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RefundDepositService {

	private final UserRepository userRepository;
	private final DepositHistoryRepository depositHistoryRepository;
	private final ProcessedEventRepository processedEventRepository;

	@Transactional
	public void refund(UUID eventId, UUID userId, BigDecimal amount, UUID orderId) {
		if (eventId != null && processedEventRepository.existsById(eventId)) {
			return;
		}
		User user = userRepository.findById(userId)
			.orElseThrow(() -> new DepositException(DepositErrorCode.NOT_FOUND_USER));

		user.chargeDeposit(amount);

		DepositHistory history = DepositHistory.of(user, orderId, amount, DepositType.REFUND);
		history.updateStatus(DepositStatus.COMPLETED);
		depositHistoryRepository.save(history);
		if (eventId != null) {
			processedEventRepository.save(ProcessedEvent.of(eventId));
		}
	}
}
