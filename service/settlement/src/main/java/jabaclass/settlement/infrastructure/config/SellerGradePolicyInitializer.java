package jabaclass.settlement.infrastructure.config;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import jabaclass.settlement.domain.model.grade.SellerGradePolicy;
import jabaclass.settlement.domain.model.grade.SellerGradeType;
import jabaclass.settlement.infrastructure.persistence.SellerGradePolicyJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@Profile({"dev", "prod"})
@RequiredArgsConstructor
public class SellerGradePolicyInitializer implements ApplicationRunner {

	private final SellerGradePolicyJpaRepository sellerGradePolicyJpaRepository;

	@Override
	@Transactional
	public void run(ApplicationArguments args) {
		if (sellerGradePolicyJpaRepository.count() > 0) {
			log.info("SellerGradePolicy 시드 생략: 기존 정책이 이미 존재합니다. count={}", sellerGradePolicyJpaRepository.count());
			return;
		}

		LocalDateTime appliedFrom = LocalDateTime.of(2026, 1, 1, 0, 0);
		List<SellerGradePolicy> policies = List.of(
			new SellerGradePolicy(
				SellerGradeType.BASIC,
				1,
				new BigDecimal("0.00"),
				new BigDecimal("499999.99"),
				new BigDecimal("0.0600"),
				true,
				appliedFrom,
				null
			),
			new SellerGradePolicy(
				SellerGradeType.SILVER,
				1,
				new BigDecimal("500000.00"),
				new BigDecimal("1999999.99"),
				new BigDecimal("0.0550"),
				true,
				appliedFrom,
				null
			),
			new SellerGradePolicy(
				SellerGradeType.GOLD,
				1,
				new BigDecimal("2000000.00"),
				new BigDecimal("4999999.99"),
				new BigDecimal("0.0500"),
				true,
				appliedFrom,
				null
			),
			new SellerGradePolicy(
				SellerGradeType.PLATINUM,
				1,
				new BigDecimal("5000000.00"),
				new BigDecimal("9999999.99"),
				new BigDecimal("0.0450"),
				true,
				appliedFrom,
				null
			),
			new SellerGradePolicy(
				SellerGradeType.DIAMOND,
				1,
				new BigDecimal("10000000.00"),
				null,
				new BigDecimal("0.0400"),
				true,
				appliedFrom,
				null
			)
		);

		sellerGradePolicyJpaRepository.saveAll(policies);
		log.info("SellerGradePolicy dev 시드 완료. count={}", policies.size());
	}
}
