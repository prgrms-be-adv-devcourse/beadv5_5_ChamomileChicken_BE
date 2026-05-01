package jabaclass.settlement.domain.repository;

import java.util.List;
import java.util.Optional;

import jabaclass.settlement.domain.model.promotion.PromotionType;
import jabaclass.settlement.domain.model.promotion.SettlementPromotion;

public interface SettlementPromotionRepository {

	List<SettlementPromotion> findAllActive();

	Optional<SettlementPromotion> findActiveByPromotionType(PromotionType promotionType);
}
