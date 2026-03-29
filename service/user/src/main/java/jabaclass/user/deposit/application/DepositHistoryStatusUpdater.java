package jabaclass.user.deposit.application;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import jabaclass.user.deposit.domain.DepositHistory;
import jabaclass.user.deposit.domain.DepositStatus;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class DepositHistoryStatusUpdater {

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void updateFailed(DepositHistory depositHistory) {
		depositHistory.updateStatus(DepositStatus.FAILED);
	}
}
