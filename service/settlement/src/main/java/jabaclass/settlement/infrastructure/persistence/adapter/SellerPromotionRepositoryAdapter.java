package jabaclass.settlement.infrastructure.persistence.adapter;

import java.time.LocalDateTime;
import java.util.List;
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
	public List<SellerPromotion> findActiveApplicablePromotions(
		List<UUID> sellerIds,
		LocalDateTime minOccurredAt,
		LocalDateTime maxOccurredAt
	) {
		if (sellerIds == null || sellerIds.isEmpty() || minOccurredAt == null || maxOccurredAt == null) {
			return List.of();
		}

		return sellerPromotionJpaRepository.findActiveApplicablePromotionsBySellerIds(
			sellerIds,
			minOccurredAt,
			maxOccurredAt
		);
	}

	@Override
	public boolean existsBySellerIdAndPromotionIdAndStartedAt(
		UUID sellerId,
		UUID promotionId,
		LocalDateTime startedAt
	) {
		return sellerPromotionJpaRepository.existsBySellerIdAndPromotionIdAndStartedAt(
			sellerId,
			promotionId,
			startedAt
		);
	}

	@Override
	public SellerPromotion save(SellerPromotion sellerPromotion) {
		return sellerPromotionJpaRepository.save(sellerPromotion);
	}
}
