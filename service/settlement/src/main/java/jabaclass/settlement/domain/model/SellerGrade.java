package jabaclass.settlement.domain.model;

import java.util.Objects;
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
@Table(name = "seller_grades")
public class SellerGrade extends BaseEntity {

	@Column(name = "seller_id", nullable = false, unique = true)
	private UUID sellerId;

	@Column(name = "seller_grade_policy_id", nullable = false)
	private UUID sellerGradePolicyId;

	@Column(name = "calculated_month", nullable = false, length = 7)
	private String calculatedMonth;

	public SellerGrade(
		UUID sellerId,
		UUID sellerGradePolicyId,
		String calculatedMonth
	) {
		validateSellerId(sellerId);
		validateSellerGradePolicyId(sellerGradePolicyId);
		validateCalculatedMonth(calculatedMonth);

		this.sellerId = sellerId;
		this.sellerGradePolicyId = sellerGradePolicyId;
		this.calculatedMonth = calculatedMonth;
	}

	public static SellerGrade create(
		UUID sellerId,
		UUID sellerGradePolicyId,
		String calculatedMonth
	) {
		return new SellerGrade(sellerId, sellerGradePolicyId, calculatedMonth);
	}

	public void update(
		UUID sellerGradePolicyId,
		String calculatedMonth
	) {
		validateSellerGradePolicyId(sellerGradePolicyId);
		validateCalculatedMonth(calculatedMonth);

		this.sellerGradePolicyId = sellerGradePolicyId;
		this.calculatedMonth = calculatedMonth;
	}

	private void validateSellerId(UUID sellerId) {
		if (sellerId == null) {
			throw new IllegalArgumentException("판매자 ID는 null일 수 없습니다.");
		}
	}

	private void validateSellerGradePolicyId(UUID sellerGradePolicyId) {
		if (sellerGradePolicyId == null) {
			throw new IllegalArgumentException("판매자 등급 정책 ID는 null일 수 없습니다.");
		}
	}

	private void validateCalculatedMonth(String calculatedMonth) {
		if (calculatedMonth == null || calculatedMonth.isBlank()) {
			throw new IllegalArgumentException("등급 산정 월은 비어 있을 수 없습니다.");
		}
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof SellerGrade that)) return false;
		return Objects.equals(getId(), that.getId());
	}

	@Override
	public int hashCode() {
		return Objects.hash(getId());
	}
}
