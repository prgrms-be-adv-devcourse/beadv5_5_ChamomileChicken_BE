package jabaclass.settlement.domain.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import jabaclass.settlement.domain.model.promotion.SellerPromotion;

public interface SellerPromotionRepository {

	Optional<SellerPromotion> findActiveApplicablePromotion(UUID sellerId, LocalDateTime occurredAt);

	List<SellerPromotion> findActiveApplicablePromotions(
		List<UUID> sellerIds,
		LocalDateTime minOccurredAt,
		LocalDateTime maxOccurredAt
	);

	boolean existsBySellerIdAndPromotionIdAndStartedAt(UUID sellerId, UUID promotionId, LocalDateTime startedAt);

	SellerPromotion save(SellerPromotion sellerPromotion);
}
