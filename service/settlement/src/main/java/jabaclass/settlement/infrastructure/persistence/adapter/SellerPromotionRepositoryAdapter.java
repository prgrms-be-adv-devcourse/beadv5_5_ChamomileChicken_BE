package jabaclass.settlement.infrastructure.persistence.adapter;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import jabaclass.settlement.domain.model.SellerPromotion;
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
}
