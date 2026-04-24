package jabaclass.settlement.domain.model.promotion;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator.ReplaceUnderscores;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

@SuppressWarnings("NonAsciiCharacters")
@DisplayNameGeneration(ReplaceUnderscores.class)
class SellerPromotionTest {

	@Test
	void 신규셀러_30일_프로모션은_마지막날_끝시각까지_적용된다() {
		LocalDateTime startedAt = LocalDateTime.of(2026, 4, 24, 10, 0);
		SettlementPromotion promotion = new SettlementPromotion(
			"신규 가입 셀러 30일 우대 수수료",
			PromotionType.NEW_SELLER,
			new BigDecimal("0.0300"),
			30,
			true
		);
		ReflectionTestUtils.setField(promotion, "id", UUID.randomUUID());

		SellerPromotion sellerPromotion = SellerPromotion.assign(UUID.randomUUID(), promotion, startedAt);

		assertThat(sellerPromotion.getStartedAt()).isEqualTo(startedAt);
		assertThat(sellerPromotion.getEndedAt()).isEqualTo(startedAt.plusDays(30).minusNanos(1));
	}
}
