package jabaclass.settlement.application.service.promotion;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator.ReplaceUnderscores;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import jabaclass.settlement.application.exception.BusinessException;
import jabaclass.settlement.domain.model.promotion.PromotionType;
import jabaclass.settlement.domain.model.promotion.SellerPromotion;
import jabaclass.settlement.domain.model.promotion.SettlementPromotion;
import jabaclass.settlement.domain.repository.SellerPromotionRepository;
import jabaclass.settlement.domain.repository.SettlementPromotionRepository;

@SuppressWarnings("NonAsciiCharacters")
@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(ReplaceUnderscores.class)
class NewSellerPromotionServiceTest {

	@Mock
	private SettlementPromotionRepository settlementPromotionRepository;

	@Mock
	private SellerPromotionRepository sellerPromotionRepository;

	@InjectMocks
	private NewSellerPromotionService newSellerPromotionService;

	@Test
	void 같은_seller와_approvedAt이면_비활성_상태여도_중복_등록하지_않는다() {
		UUID sellerId = UUID.randomUUID();
		UUID promotionId = UUID.randomUUID();
		LocalDateTime approvedAt = LocalDateTime.of(2026, 4, 24, 10, 0);
		SettlementPromotion promotion = new SettlementPromotion(
			"신규 가입 셀러 30일 우대 수수료",
			PromotionType.NEW_SELLER,
			new BigDecimal("0.0300"),
			30,
			true
		);
		ReflectionTestUtils.setField(promotion, "id", promotionId);

		given(settlementPromotionRepository.findActiveByPromotionType(PromotionType.NEW_SELLER))
			.willReturn(Optional.of(promotion));
		given(sellerPromotionRepository.existsBySellerIdAndPromotionIdAndStartedAt(sellerId, promotionId, approvedAt))
			.willReturn(true);

		newSellerPromotionService.register(sellerId, approvedAt);

		then(sellerPromotionRepository).should(never()).save(any(SellerPromotion.class));
	}

	@Test
	void 활성_NEW_SELLER_프로모션이_없으면_예외가_발생한다() {
		UUID sellerId = UUID.randomUUID();
		LocalDateTime approvedAt = LocalDateTime.of(2026, 4, 24, 10, 0);

		given(settlementPromotionRepository.findActiveByPromotionType(PromotionType.NEW_SELLER))
			.willReturn(Optional.empty());

		assertThatThrownBy(() -> newSellerPromotionService.register(sellerId, approvedAt))
			.isInstanceOf(BusinessException.class)
			.hasMessage("정산 프로모션 정보를 찾을 수 없습니다.");
	}

	@Test
	void 같은_seller와_approvedAt_기록이_없으면_프로모션을_등록한다() {
		UUID sellerId = UUID.randomUUID();
		UUID promotionId = UUID.randomUUID();
		LocalDateTime approvedAt = LocalDateTime.of(2026, 4, 24, 10, 0);
		SettlementPromotion promotion = new SettlementPromotion(
			"신규 가입 셀러 30일 우대 수수료",
			PromotionType.NEW_SELLER,
			new BigDecimal("0.0300"),
			30,
			true
		);
		ReflectionTestUtils.setField(promotion, "id", promotionId);

		given(settlementPromotionRepository.findActiveByPromotionType(PromotionType.NEW_SELLER))
			.willReturn(Optional.of(promotion));
		given(sellerPromotionRepository.existsBySellerIdAndPromotionIdAndStartedAt(sellerId, promotionId, approvedAt))
			.willReturn(false);

		newSellerPromotionService.register(sellerId, approvedAt);

		then(sellerPromotionRepository).should().save(any(SellerPromotion.class));
	}
}
