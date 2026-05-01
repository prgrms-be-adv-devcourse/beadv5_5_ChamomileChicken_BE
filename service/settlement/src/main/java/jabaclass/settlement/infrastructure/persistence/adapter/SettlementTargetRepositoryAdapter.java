package jabaclass.settlement.infrastructure.persistence.adapter;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import jabaclass.settlement.domain.model.settlement.SettlementTarget;
import jabaclass.settlement.domain.model.settlement.SettlementTargetType;
import jabaclass.settlement.domain.repository.SettlementTargetRepository;
import jabaclass.settlement.infrastructure.persistence.SettlementTargetJpaRepository;
import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class SettlementTargetRepositoryAdapter implements SettlementTargetRepository {

	private final SettlementTargetJpaRepository settlementTargetJpaRepository;

	@Override
	public List<SettlementTarget> saveAll(List<SettlementTarget> settlementTargets) {
		return settlementTargetJpaRepository.saveAll(settlementTargets);
	}

	@Override
	public SettlementTarget save(SettlementTarget settlementTarget) {
		return settlementTargetJpaRepository.save(settlementTarget);
	}

	@Override
	public List<SettlementTarget> findByPaymentIdsAndTargetType(List<UUID> paymentIds, SettlementTargetType targetType) {
		if (paymentIds == null || paymentIds.isEmpty()) {
			return List.of();
		}

		return settlementTargetJpaRepository.findByPaymentIdInAndTargetType(paymentIds, targetType);
	}

	@Override
	public List<SettlementTarget> findAllByIds(List<UUID> ids) {
		if (ids == null || ids.isEmpty()) {
			return List.of();
		}

		return settlementTargetJpaRepository.findByIdIn(ids);
	}
}
