package jabaclass.settlement.application.service.query;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import jabaclass.settlement.application.exception.BusinessException;
import jabaclass.settlement.application.exception.SettlementErrorCode;
import jabaclass.settlement.application.usecase.SettlementUseCase;
import jabaclass.settlement.domain.model.settlement.Settlement;
import jabaclass.settlement.domain.model.settlement.SettlementStatus;
import jabaclass.settlement.domain.repository.SettlementRepository;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Service
public class SettlementService implements SettlementUseCase {

	private final SettlementRepository settlementRepository;

	@Override
	public List<Settlement> getSettlementsByMonth(String settlementMonth) {
		return settlementRepository.findBySettlementMonth(settlementMonth);
	}

	@Override
	public List<Settlement> getReadySettlementsByMonth(String settlementMonth) {
		return settlementRepository.findBySettlementMonthAndStatus(
			settlementMonth,
			SettlementStatus.READY
		);
	}

	@Override
	public Settlement getSettlement(UUID settlementId) {
		return settlementRepository.findById(settlementId)
			.orElseThrow(() -> new BusinessException(SettlementErrorCode.SETTLEMENT_NOT_FOUND));
	}
}
