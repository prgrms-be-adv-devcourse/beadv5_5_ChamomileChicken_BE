package jabaclass.settlement.application.service.promotion;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jabaclass.settlement.application.exception.BusinessException;
import jabaclass.settlement.application.exception.SettlementErrorCode;
import jabaclass.settlement.domain.model.promotion.PromotionType;
import jabaclass.settlement.domain.model.promotion.SellerPromotion;
import jabaclass.settlement.domain.model.promotion.SettlementPromotion;
import jabaclass.settlement.domain.repository.SellerPromotionRepository;
import jabaclass.settlement.domain.repository.SettlementPromotionRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class NewSellerPromotionService {

	private final SettlementPromotionRepository settlementPromotionRepository;
	private final SellerPromotionRepository sellerPromotionRepository;

	@Transactional
	public void register(UUID sellerId, LocalDateTime approvedAt) {
		SettlementPromotion promotion = settlementPromotionRepository.findActiveByPromotionType(PromotionType.NEW_SELLER)
			.orElseThrow(() -> new BusinessException(SettlementErrorCode.SETTLEMENT_PROMOTION_NOT_FOUND));

		if (sellerPromotionRepository.existsBySellerIdAndPromotionIdAndStartedAt(
			sellerId,
			promotion.getId(),
			approvedAt
		)) {
			return;
		}

		sellerPromotionRepository.save(
			SellerPromotion.assign(sellerId, promotion, approvedAt)
		);
	}
}
