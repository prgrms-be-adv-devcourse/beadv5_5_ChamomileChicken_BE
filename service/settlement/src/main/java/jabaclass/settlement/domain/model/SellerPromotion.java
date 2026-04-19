package jabaclass.settlement.domain.model;

import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "seller_promotions")
public class SellerPromotion extends BaseEntity {

	@Column(name = "seller_id", nullable = false)
	private UUID sellerId;

	@Column(name = "promotion_id", nullable = false)
	private UUID promotionId;

	@Column(name = "started_at", nullable = false)
	private LocalDateTime startedAt;

	@Column(name = "ended_at")
	private LocalDateTime endedAt;

	@Column(name = "active", nullable = false)
	private boolean active;

	private SellerPromotion(
		UUID sellerId,
		UUID promotionId,
		LocalDateTime startedAt,
		LocalDateTime endedAt,
		boolean active
	) {
		validateRequiredId(sellerId, "판매자 ID");
		validateRequiredId(promotionId, "프로모션 ID");
		validateStartedAt(startedAt);

		this.sellerId = sellerId;
		this.promotionId = promotionId;
		this.startedAt = startedAt;
		this.endedAt = endedAt;
		this.active = active;
	}

	public static SellerPromotion assign(
		UUID sellerId,
		SettlementPromotion promotion,
		LocalDateTime startedAt
	) {
		validatePromotion(promotion);
		validateStaticStartedAt(startedAt);

		return new SellerPromotion(
			sellerId,
			promotion.getId(),
			startedAt,
			startedAt.plusDays((long) promotion.getDurationDays() - 1),
			true
		);
	}

	private void validateRequiredId(UUID id, String fieldName) {
		if (id == null) {
			throw new IllegalArgumentException(fieldName + "는 null일 수 없습니다.");
		}
	}

	private void validateStartedAt(LocalDateTime startedAt) {
		if (startedAt == null) {
			throw new IllegalArgumentException("프로모션 시작일은 null일 수 없습니다.");
		}
	}

	private static void validatePromotion(SettlementPromotion promotion) {
		if (promotion == null) {
			throw new IllegalArgumentException("프로모션 정보는 null일 수 없습니다.");
		}
		if (promotion.getId() == null) {
			throw new IllegalArgumentException("프로모션 ID는 null일 수 없습니다.");
		}
	}

	private static void validateStaticStartedAt(LocalDateTime startedAt) {
		if (startedAt == null) {
			throw new IllegalArgumentException("프로모션 시작일은 null일 수 없습니다.");
		}
	}
}
