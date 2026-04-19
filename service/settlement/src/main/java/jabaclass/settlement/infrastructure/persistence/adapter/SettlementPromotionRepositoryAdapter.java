package jabaclass.settlement.infrastructure.persistence.adapter;

import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import jabaclass.settlement.domain.model.SettlementPromotion;
import jabaclass.settlement.domain.repository.SettlementPromotionRepository;
import jabaclass.settlement.infrastructure.persistence.SettlementPromotionJpaRepository;
import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class SettlementPromotionRepositoryAdapter implements SettlementPromotionRepository {

	private final SettlementPromotionJpaRepository settlementPromotionJpaRepository;

	@Override
	public Optional<SettlementPromotion> findById(UUID promotionId) {
		return settlementPromotionJpaRepository.findById(promotionId);
	}
}
