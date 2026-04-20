package jabaclass.settlement.application.service.calculation;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import jabaclass.settlement.application.dto.AppliedPromotion;
import jabaclass.settlement.domain.model.promotion.SellerPromotion;
import jabaclass.settlement.domain.model.promotion.SettlementPromotion;
import jabaclass.settlement.domain.repository.SellerPromotionRepository;
import jabaclass.settlement.domain.repository.SettlementPromotionRepository;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SettlementPromotionResolver {

	private final SettlementPromotionRepository settlementPromotionRepository;
	private final SellerPromotionRepository sellerPromotionRepository;

	public AppliedPromotion resolve(UUID sellerId, LocalDateTime occurredAt) {
		return sellerPromotionRepository.findActiveApplicablePromotion(sellerId, occurredAt)
			.flatMap(this::toAppliedPromotion)
			.orElseGet(AppliedPromotion::empty);
	}

	private Optional<AppliedPromotion> toAppliedPromotion(SellerPromotion sellerPromotion) {
		return settlementPromotionRepository.findById(sellerPromotion.getPromotionId())
			.filter(SettlementPromotion::isActive)
			.map(promotion -> new AppliedPromotion(
				promotion.getId(),
				promotion.getPromotionType().name(),
				promotion.getFeeRate()
			));
	}
}
