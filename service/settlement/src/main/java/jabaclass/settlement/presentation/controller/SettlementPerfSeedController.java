package jabaclass.settlement.presentation.controller;

import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jabaclass.settlement.infrastructure.perf.SettlementPerfDataSeeder;
import lombok.RequiredArgsConstructor;

@RestController
@Profile("dev-local")
@RequestMapping("/api/v1/dev/settlements/perf")
@RequiredArgsConstructor
public class SettlementPerfSeedController {

	private final SettlementPerfDataSeeder settlementPerfDataSeeder;

	@PostMapping("/seed")
	public String seed() {
		settlementPerfDataSeeder.seed();
		return "정산 성능 테스트 데이터 적재 완료";
	}
}
