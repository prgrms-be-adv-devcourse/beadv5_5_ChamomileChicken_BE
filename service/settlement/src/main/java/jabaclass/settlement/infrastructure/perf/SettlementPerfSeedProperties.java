package jabaclass.settlement.infrastructure.perf;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Component
@Profile("dev-local")
@ConfigurationProperties(prefix = "settlement.perf.seed")
public class SettlementPerfSeedProperties {

	private boolean enabled = false;
	private boolean truncateBeforeSeed = false;
	private int sellerCount = 10_000;
	private int targetMonthPaymentCount = 1_000_000;
	private int previousMonthPaymentCount = 300_000;
	private int twoMonthsAgoPaymentCount = 200_000;
	private double refundRatio = 0.12d;
	private double promotedSellerRatio = 0.08d;
	private double existingSettlementRatio = 0.25d;
	private int batchSize = 5_000;
	private String targetMonth = "2026-04";
	private long randomSeed = 202604L;
}
