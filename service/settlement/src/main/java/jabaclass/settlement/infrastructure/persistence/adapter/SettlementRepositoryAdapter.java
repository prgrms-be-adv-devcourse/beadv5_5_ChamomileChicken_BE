package jabaclass.settlement.infrastructure.persistence.adapter;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import jabaclass.settlement.domain.model.settlement.Settlement;
import jabaclass.settlement.domain.model.settlement.SettlementStatus;
import jabaclass.settlement.domain.repository.SettlementRepository;
import jabaclass.settlement.infrastructure.persistence.SettlementJpaRepository;
import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class SettlementRepositoryAdapter implements SettlementRepository {

	private final SettlementJpaRepository settlementJpaRepository;

	@Override
	public Settlement save(Settlement settlement) {
		return settlementJpaRepository.save(settlement);
	}

	@Override
	public List<Settlement> saveAll(List<Settlement> settlements) {
		return settlementJpaRepository.saveAll(settlements);
	}

	@Override
	public Optional<Settlement> findById(UUID settlementId) {
		return settlementJpaRepository.findById(settlementId);
	}

	@Override
	public Page<Settlement> findBySellerId(UUID sellerId, Pageable pageable) {
		return settlementJpaRepository.findBySellerId(sellerId, pageable);
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
