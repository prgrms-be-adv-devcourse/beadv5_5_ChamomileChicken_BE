package jabaclass.settlement.domain.model;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "settlement_promotions")
public class SettlementPromotion extends BaseEntity {

	@Column(name = "name", nullable = false, length = 100)
	private String name;

	@Enumerated(EnumType.STRING)
	@Column(name = "promotion_type", nullable = false, length = 30)
	private PromotionType promotionType;

	@Column(name = "fee_rate", nullable = false, precision = 10, scale = 4)
	private BigDecimal feeRate;

	@Column(name = "duration_days", nullable = false)
	private Integer durationDays;

	@Column(name = "active", nullable = false)
	private boolean active;

	public SettlementPromotion(
		String name,
		PromotionType promotionType,
		BigDecimal feeRate,
		Integer durationDays,
		boolean active
	) {
		validateName(name);
		validatePromotionType(promotionType);
		validateFeeRate(feeRate);
		validateDurationDays(durationDays);

		this.name = name;
		this.promotionType = promotionType;
		this.feeRate = feeRate;
		this.durationDays = durationDays;
		this.active = active;
	}

	private void validateName(String name) {
		if (name == null || name.isBlank()) {
			throw new IllegalArgumentException("프로모션명은 비어 있을 수 없습니다.");
		}
	}

	private void validatePromotionType(PromotionType promotionType) {
		if (promotionType == null) {
			throw new IllegalArgumentException("프로모션 타입은 비어 있을 수 없습니다.");
		}
	}

	private void validateFeeRate(BigDecimal feeRate) {
		if (feeRate == null) {
			throw new IllegalArgumentException("프로모션 수수료율은 null일 수 없습니다.");
		}
	}

	private void validateDurationDays(Integer durationDays) {
		if (durationDays == null || durationDays <= 0) {
			throw new IllegalArgumentException("프로모션 기간 일수는 1 이상이어야 합니다.");
		}
	}
}
