package jabaclass.settlement.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jabaclass.settlement.domain.model.settlement.Settlement;
import jabaclass.settlement.domain.model.settlement.SettlementTransfer;
import jabaclass.settlement.domain.repository.SettlementRepository;
import jabaclass.settlement.domain.repository.SettlementTransferRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SettlementTransferStatePersistenceService {

	private final SettlementRepository settlementRepository;
	private final SettlementTransferRepository settlementTransferRepository;

	@Transactional
	public void saveTransferState(Settlement settlement, SettlementTransfer transferHistory) {
		settlementRepository.save(settlement);
		settlementTransferRepository.save(transferHistory);
	}
}
