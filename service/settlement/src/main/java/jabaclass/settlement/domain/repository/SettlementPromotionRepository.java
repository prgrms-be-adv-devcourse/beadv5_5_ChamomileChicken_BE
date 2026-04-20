package jabaclass.settlement.domain.repository;

import java.util.Optional;
import java.util.UUID;

import jabaclass.settlement.domain.model.promotion.SettlementPromotion;

public interface SettlementPromotionRepository {

	Optional<SettlementPromotion> findById(UUID promotionId);
}
