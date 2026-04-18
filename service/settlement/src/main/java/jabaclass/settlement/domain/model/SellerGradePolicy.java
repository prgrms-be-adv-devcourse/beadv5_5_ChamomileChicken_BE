package jabaclass.settlement.domain.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Objects;

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
@Table(name = "seller_grade_policies")
public class SellerGradePolicy extends BaseEntity {

	@Enumerated(EnumType.STRING)
	@Column(name = "grade_code", nullable = false, length = 30)
	private SellerGradeType gradeCode;

	@Column(name = "version", nullable = false)
	private Integer version;

	@Column(name = "min_sales_amount", nullable = false, precision = 19, scale = 2)
	private BigDecimal minSalesAmount;

	@Column(name = "max_sales_amount", precision = 19, scale = 2)
	private BigDecimal maxSalesAmount;

	@Column(name = "fee_rate", nullable = false, precision = 10, scale = 4)
	private BigDecimal feeRate;

	@Column(name = "active", nullable = false)
	private boolean active;

	@Column(name = "applied_from", nullable = false)
	private LocalDateTime appliedFrom;

	@Column(name = "applied_to")
	private LocalDateTime appliedTo;

	public SellerGradePolicy(
		SellerGradeType gradeCode,
		Integer version,
		BigDecimal minSalesAmount,
		BigDecimal maxSalesAmount,
		BigDecimal feeRate,
		boolean active,
		LocalDateTime appliedFrom,
		LocalDateTime appliedTo
	) {
		validateGradeCode(gradeCode);
		validateVersion(version);
		validateAmount(minSalesAmount, "최소 판매 금액");
		validateAmount(feeRate, "수수료율");
		validateAppliedFrom(appliedFrom);

		this.gradeCode = gradeCode;
		this.version = version;
		this.minSalesAmount = minSalesAmount;
		this.maxSalesAmount = maxSalesAmount;
		this.feeRate = feeRate;
		this.active = active;
		this.appliedFrom = appliedFrom;
		this.appliedTo = appliedTo;
	}

	public SettlementAmount calculateSettlementAmount(BigDecimal originalAmount) {
		validateAmount(originalAmount, "정산 원금");

		BigDecimal feeAmount = originalAmount
			.multiply(feeRate)
			.setScale(2, RoundingMode.DOWN);

		return new SettlementAmount(
			originalAmount,
			feeAmount,
			feeRate,
			originalAmount.subtract(feeAmount)
		);
	}

	public boolean matches(BigDecimal salesAmount) {
		validateAmount(salesAmount, "판매 금액");

		boolean meetsMinimum = salesAmount.compareTo(minSalesAmount) >= 0;
		boolean withinMaximum = maxSalesAmount == null || salesAmount.compareTo(maxSalesAmount) <= 0;
		return meetsMinimum && withinMaximum;
	}

	private void validateGradeCode(SellerGradeType gradeCode) {
		if (gradeCode == null) {
			throw new IllegalArgumentException("판매자 등급 코드는 비어 있을 수 없습니다.");
		}
	}

	private void validateVersion(Integer version) {
		if (version == null || version <= 0) {
			throw new IllegalArgumentException("정책 버전은 1 이상이어야 합니다.");
		}
	}

	private void validateAmount(BigDecimal amount, String fieldName) {
		if (amount == null) {
			throw new IllegalArgumentException(fieldName + "은 null일 수 없습니다.");
		}
	}

	private void validateAppliedFrom(LocalDateTime appliedFrom) {
		if (appliedFrom == null) {
			throw new IllegalArgumentException("정책 적용 시작 시각은 null일 수 없습니다.");
		}
	}

	public record SettlementAmount(
		BigDecimal originalAmount,
		BigDecimal feeAmount,
		BigDecimal feeRate,
		BigDecimal settlementAmount
	) {
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof SellerGradePolicy that)) return false;
		return Objects.equals(getId(), that.getId());
	}

	@Override
	public int hashCode() {
		return Objects.hash(getId());
	}
}
