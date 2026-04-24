package jabaclass.settlement.infrastructure.persistence.adapter;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import jabaclass.settlement.domain.model.promotion.SellerPromotion;
import jabaclass.settlement.domain.repository.SellerPromotionRepository;
import jabaclass.settlement.infrastructure.persistence.SellerPromotionJpaRepository;
import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class SellerPromotionRepositoryAdapter implements SellerPromotionRepository {

	private final SellerPromotionJpaRepository sellerPromotionJpaRepository;

	@Override
	public Optional<SellerPromotion> findActiveApplicablePromotion(UUID sellerId, LocalDateTime occurredAt) {
		if (sellerId == null || occurredAt == null) {
			return Optional.empty();
		}

		return sellerPromotionJpaRepository.findActiveApplicablePromotions(sellerId, occurredAt)
			.stream()
			.findFirst();
	}

	@Override
	public boolean existsActiveBySellerIdAndPromotionId(UUID sellerId, UUID promotionId) {
		return sellerPromotionJpaRepository.existsBySellerIdAndPromotionIdAndActiveTrue(sellerId, promotionId);
	}

	@Override
	public SellerPromotion save(SellerPromotion sellerPromotion) {
		return sellerPromotionJpaRepository.save(sellerPromotion);
	}
}
