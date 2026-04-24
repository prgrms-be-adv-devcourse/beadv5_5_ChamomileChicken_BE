package jabaclass.settlement.domain.repository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import jabaclass.settlement.domain.model.promotion.SellerPromotion;

public interface SellerPromotionRepository {

	Optional<SellerPromotion> findActiveApplicablePromotion(UUID sellerId, LocalDateTime occurredAt);

	boolean existsActiveBySellerIdAndPromotionId(UUID sellerId, UUID promotionId);

	SellerPromotion save(SellerPromotion sellerPromotion);
}
