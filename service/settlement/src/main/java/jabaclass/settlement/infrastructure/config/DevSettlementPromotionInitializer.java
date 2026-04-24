package jabaclass.settlement.infrastructure.config;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import jabaclass.settlement.domain.model.promotion.PromotionType;
import jabaclass.settlement.domain.model.promotion.SettlementPromotion;
import jabaclass.settlement.infrastructure.persistence.SettlementPromotionJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
//@Profile({"dev", "prod"})
@Profile("dev")
@RequiredArgsConstructor
public class DevSettlementPromotionInitializer implements ApplicationRunner {

	private final SettlementPromotionJpaRepository settlementPromotionJpaRepository;

	@Override
	@Transactional
	public void run(ApplicationArguments args) {
		if (settlementPromotionJpaRepository.count() > 0) {
			log.info("SettlementPromotion 시드 생략: 기존 프로모션이 이미 존재합니다. count={}",
				settlementPromotionJpaRepository.count());
			return;
		}

		List<SettlementPromotion> promotions = List.of(
			new SettlementPromotion(
				"신규 가입 셀러 30일 우대 수수료",
				PromotionType.NEW_SELLER,
				new BigDecimal("0.0300"),
				30,
				true
			)
		);

		settlementPromotionJpaRepository.saveAll(promotions);
		log.info("SettlementPromotion 시드 완료. count={}", promotions.size());
	}
}
