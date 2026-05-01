package jabaclass.settlement.infrastructure.persistence.adapter;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import jabaclass.settlement.domain.model.promotion.PromotionType;
import jabaclass.settlement.domain.model.promotion.SettlementPromotion;
import jabaclass.settlement.domain.repository.SettlementPromotionRepository;
import jabaclass.settlement.infrastructure.persistence.SettlementPromotionJpaRepository;
import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class SettlementPromotionRepositoryAdapter implements SettlementPromotionRepository {

	private final SettlementPromotionJpaRepository settlementPromotionJpaRepository;

	@Override
	public List<SettlementPromotion> findAllActive() {
		return settlementPromotionJpaRepository.findAllByActiveTrue();
	}

	@Override
	public Optional<SettlementPromotion> findActiveByPromotionType(PromotionType promotionType) {
		return settlementPromotionJpaRepository.findFirstByPromotionTypeAndActiveTrue(promotionType);
	}
}
