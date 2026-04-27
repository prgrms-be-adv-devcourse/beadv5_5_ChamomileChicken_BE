package jabaclass.settlement.infrastructure.persistence.adapter;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import jabaclass.settlement.domain.model.settlement.SettlementTransfer;
import jabaclass.settlement.domain.repository.SettlementTransferRepository;
import jabaclass.settlement.infrastructure.persistence.SettlementTransferJpaRepository;
import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class SettlementTransferRepositoryAdapter implements SettlementTransferRepository {

	private final SettlementTransferJpaRepository settlementTransferJpaRepository;

	@Override
	public SettlementTransfer save(SettlementTransfer settlementTransfer) {
		return settlementTransferJpaRepository.save(settlementTransfer);
	}

	@Override
	public List<SettlementTransfer> saveAll(List<SettlementTransfer> settlementTransfers) {
		return settlementTransferJpaRepository.saveAll(settlementTransfers);
	}

	@Override
	public Optional<SettlementTransfer> findLatestBySettlementId(UUID settlementId) {
		return settlementTransferJpaRepository.findTopBySettlementIdOrderByRequestedAtDesc(settlementId);
	}
}
