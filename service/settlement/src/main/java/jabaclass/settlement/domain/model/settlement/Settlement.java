package jabaclass.settlement.domain.model.settlement;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import jabaclass.settlement.domain.model.BaseEntity;
import jabaclass.settlement.domain.model.grade.SellerGradeType;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "settlements")
public class Settlement extends BaseEntity {

	@Column(name = "seller_id", nullable = false)
	private UUID sellerId;

	/**
	 * 형식: yyyy-MM
	 * 예: 2026-03
	 */
	@Column(name = "settlement_month", nullable = false, length = 7)
	private String settlementMonth;

	@Column(name = "original_amount", nullable = false, precision = 19, scale = 2)
	private BigDecimal originalAmount;

	@Column(name = "seller_grade_code", nullable = false, length = 30)
	@Enumerated(EnumType.STRING)
	private SellerGradeType sellerGradeCode;

	@Column(name = "seller_grade_policy_id", nullable = false)
	private UUID sellerGradePolicyId;

	@Column(name = "grade_base_amount", nullable = false, precision = 19, scale = 2)
	private BigDecimal gradeBaseAmount;

	@Column(name = "fee_amount", nullable = false, precision = 19, scale = 2)
	private BigDecimal feeAmount;

	@Column(name = "fee_rate", nullable = false, precision = 10, scale = 4)
	private BigDecimal feeRate;

	@Column(name = "settlement_amount", nullable = false, precision = 19, scale = 2)
	private BigDecimal settlementAmount;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 20)
	private SettlementStatus status;

	@Column(name = "transferred_at")
	private LocalDateTime transferredAt;

	@Column(name = "fail_reason", length = 500)
	private String failReason;

	public Settlement(
		UUID sellerId,
		String settlementMonth,
		BigDecimal originalAmount,
		SellerGradeType sellerGradeCode,
		UUID sellerGradePolicyId,
		BigDecimal gradeBaseAmount,
		BigDecimal feeAmount,
		BigDecimal feeRate,
		BigDecimal settlementAmount,
		SettlementStatus status
	) {
		validateSellerId(sellerId);
		validateSettlementMonth(settlementMonth);
		validateAmount(originalAmount, "정산 원금");
		validateSellerGradeCode(sellerGradeCode);
		validateSellerGradePolicyId(sellerGradePolicyId);
		validateAmount(gradeBaseAmount, "등급 산정 기준 금액");
		validateAmount(feeAmount, "수수료");
		validateAmount(feeRate, "수수료율");
		validateAmount(settlementAmount, "최종 정산금");
		validateStatus(status);

		this.sellerId = sellerId;
		this.settlementMonth = settlementMonth;
		this.originalAmount = originalAmount;
		this.sellerGradeCode = sellerGradeCode;
		this.sellerGradePolicyId = sellerGradePolicyId;
		this.gradeBaseAmount = gradeBaseAmount;
		this.feeAmount = feeAmount;
		this.feeRate = feeRate;
		this.settlementAmount = settlementAmount;
		this.status = status;
	}

	public static Settlement createReady(
		UUID sellerId,
		String settlementMonth,
		BigDecimal originalAmount,
		SellerGradeType sellerGradeCode,
		UUID sellerGradePolicyId,
		BigDecimal gradeBaseAmount,
		BigDecimal feeAmount,
		BigDecimal feeRate,
		BigDecimal settlementAmount
	) {
		return new Settlement(
			sellerId,
			settlementMonth,
			originalAmount,
			sellerGradeCode,
			sellerGradePolicyId,
			gradeBaseAmount,
			feeAmount,
			feeRate,
			settlementAmount,
			SettlementStatus.READY
		);
	}

	public void hold(String failReason) {
		this.status = SettlementStatus.HOLD;
		this.failReason = failReason;
	}

	public void recalculate(
		BigDecimal originalAmount,
		SellerGradeType sellerGradeCode,
		UUID sellerGradePolicyId,
		BigDecimal gradeBaseAmount,
		BigDecimal feeAmount,
		BigDecimal feeRate,
		BigDecimal settlementAmount
	) {
		validateAmount(originalAmount, "정산 원금");
		validateSellerGradeCode(sellerGradeCode);
		validateSellerGradePolicyId(sellerGradePolicyId);
		validateAmount(gradeBaseAmount, "등급 산정 기준 금액");
		validateAmount(feeAmount, "수수료");
		validateAmount(feeRate, "수수료율");
		validateAmount(settlementAmount, "최종 정산금");

		this.originalAmount = originalAmount;
		this.sellerGradeCode = sellerGradeCode;
		this.sellerGradePolicyId = sellerGradePolicyId;
		this.gradeBaseAmount = gradeBaseAmount;
		this.feeAmount = feeAmount;
		this.feeRate = feeRate;
		this.settlementAmount = settlementAmount;
		this.status = SettlementStatus.READY;
		this.failReason = null;
	}

	public boolean canRecalculate() {
		return status == SettlementStatus.READY
			|| status == SettlementStatus.HOLD
			|| status == SettlementStatus.FAILED;
	}

	public void markTransferring() {
		this.status = SettlementStatus.TRANSFERRING;
		this.failReason = null;
	}

	public void markSent(LocalDateTime transferredAt) {
		this.status = SettlementStatus.SENT;
		this.transferredAt = transferredAt;
		this.failReason = null;
	}

	public void markFailed(String failReason) {
		this.status = SettlementStatus.FAILED;
		this.failReason = failReason;
	}

	public boolean isTransferable() {
		return settlementAmount.compareTo(BigDecimal.ZERO) > 0;
	}

	private void validateSellerId(UUID sellerId) {
		if (sellerId == null) {
			throw new IllegalArgumentException("판매자 ID는 null일 수 없습니다.");
		}
	}

	private void validateSettlementMonth(String settlementMonth) {
		if (settlementMonth == null || settlementMonth.isBlank()) {
			throw new IllegalArgumentException("정산월은 비어 있을 수 없습니다.");
		}
	}

	private void validateAmount(BigDecimal amount, String fieldName) {
		if (amount == null) {
			throw new IllegalArgumentException(fieldName + "은 null일 수 없습니다.");
		}
	}

	private void validateSellerGradeCode(SellerGradeType sellerGradeCode) {
		if (sellerGradeCode == null) {
			throw new IllegalArgumentException("판매자 등급 코드는 비어 있을 수 없습니다.");
		}
	}

	private void validateSellerGradePolicyId(UUID sellerGradePolicyId) {
		if (sellerGradePolicyId == null) {
			throw new IllegalArgumentException("판매자 등급 정책 ID는 null일 수 없습니다.");
		}
	}

	private void validateStatus(SettlementStatus status) {
		if (status == null) {
			throw new IllegalArgumentException("정산 상태는 null일 수 없습니다.");
		}
	}
}
