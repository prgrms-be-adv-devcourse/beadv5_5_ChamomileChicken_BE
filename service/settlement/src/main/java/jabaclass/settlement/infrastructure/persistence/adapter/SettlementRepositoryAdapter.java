package jabaclass.settlement.infrastructure.persistence.adapter;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import jabaclass.settlement.domain.model.Settlement;
import jabaclass.settlement.domain.model.SettlementStatus;
import jabaclass.settlement.domain.repository.SettlementRepository;
import jabaclass.settlement.infrastructure.persistence.SettlementJpaRepository;
import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class SettlementRepositoryAdapter implements SettlementRepository {

	private final SettlementJpaRepository settlementJpaRepository;

	@Override
	public List<Settlement> saveAll(List<Settlement> settlements) {
		return settlementJpaRepository.saveAll(settlements);
	}

	@Override
	public Optional<Settlement> findById(UUID settlementId) {
		return settlementJpaRepository.findById(settlementId);
	}

	@Override
	public boolean existsBySellerIdAndSettlementMonth(UUID sellerId, String settlementMonth) {
		return settlementJpaRepository.existsBySellerIdAndSettlementMonth(sellerId, settlementMonth);
	}

	@Override
	public List<Settlement> findBySettlementMonth(String settlementMonth) {
		return settlementJpaRepository.findBySettlementMonth(settlementMonth);
	}

	@Override
	public List<Settlement> findBySettlementMonthAndStatus(String settlementMonth, SettlementStatus status) {
		return settlementJpaRepository.findBySettlementMonthAndStatus(settlementMonth, status);
	}
}
