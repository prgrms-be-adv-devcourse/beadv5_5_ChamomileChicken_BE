package jabaclass.settlement.domain.model;

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

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "settlement_targets")
public class SettlementTarget extends BaseEntity {

	@Column(name = "settlement_month", nullable = false, length = 7)
	private String settlementMonth;

	@Column(name = "seller_id", nullable = false)
	private UUID sellerId;

	@Column(name = "order_id", nullable = false)
	private UUID orderId;

	@Column(name = "payment_id")
	private UUID paymentId;

	@Column(name = "refund_id")
	private UUID refundId;

	@Column(name = "product_id", nullable = false)
	private UUID productId;

	@Enumerated(EnumType.STRING)
	@Column(name = "target_type", nullable = false, length = 20)
	private SettlementTargetType targetType;

	@Column(name = "gross_amount", nullable = false, precision = 19, scale = 2)
	private BigDecimal grossAmount;

	@Column(name = "occurred_at", nullable = false)
	private LocalDateTime occurredAt;

	@Enumerated(EnumType.STRING)
	@Column(name = "calculation_status", nullable = false, length = 20)
	private SettlementTargetCalculationStatus calculationStatus;

	@Column(name = "calculation_requested_at")
	private LocalDateTime calculationRequestedAt;

	@Column(name = "calculation_completed_at")
	private LocalDateTime calculationCompletedAt;

	@Column(name = "calculation_failed_reason", length = 500)
	private String calculationFailedReason;

	private SettlementTarget(
		String settlementMonth,
		UUID sellerId,
		UUID orderId,
		UUID paymentId,
		UUID refundId,
		UUID productId,
		SettlementTargetType targetType,
		BigDecimal grossAmount,
		LocalDateTime occurredAt
	) {
		validateSettlementMonth(settlementMonth);
		validateRequiredId(sellerId, "판매자 ID");
		validateRequiredId(orderId, "주문 ID");
		validateRequiredId(productId, "상품 ID");
		validateTargetType(targetType);
		validateAmount(grossAmount, "원천 거래 금액");
		validateOccurredAt(occurredAt);
		validateReferenceIds(targetType, paymentId, refundId);

		this.settlementMonth = settlementMonth;
		this.sellerId = sellerId;
		this.orderId = orderId;
		this.paymentId = paymentId;
		this.refundId = refundId;
		this.productId = productId;
		this.targetType = targetType;
		this.grossAmount = grossAmount;
		this.occurredAt = occurredAt;
		this.calculationStatus = SettlementTargetCalculationStatus.PENDING;
		this.calculationRequestedAt = LocalDateTime.now();
	}

	public static SettlementTarget forPayment(
		String settlementMonth,
		UUID sellerId,
		UUID orderId,
		UUID paymentId,
		UUID productId,
		BigDecimal grossAmount,
		LocalDateTime occurredAt
	) {
		return new SettlementTarget(
			settlementMonth,
			sellerId,
			orderId,
			paymentId,
			null,
			productId,
			SettlementTargetType.PAYMENT,
			grossAmount,
			occurredAt
		);
	}

	public static SettlementTarget forRefund(
		String settlementMonth,
		UUID sellerId,
		UUID orderId,
		UUID paymentId,
		UUID refundId,
		UUID productId,
		BigDecimal refundAmount,
		LocalDateTime occurredAt
	) {
		return new SettlementTarget(
			settlementMonth,
			sellerId,
			orderId,
			paymentId,
			refundId,
			productId,
			SettlementTargetType.REFUND,
			refundAmount.negate(),
			occurredAt
		);
	}

	public void markCalculated() {
		this.calculationStatus = SettlementTargetCalculationStatus.CALCULATED;
		this.calculationCompletedAt = LocalDateTime.now();
		this.calculationFailedReason = null;
	}

	public void markCalculationFailed(String reason) {
		this.calculationStatus = SettlementTargetCalculationStatus.FAILED;
		this.calculationCompletedAt = LocalDateTime.now();
		this.calculationFailedReason = reason;
	}

	private void validateSettlementMonth(String settlementMonth) {
		if (settlementMonth == null || settlementMonth.isBlank()) {
			throw new IllegalArgumentException("정산월은 비어 있을 수 없습니다.");
		}
	}

	private void validateRequiredId(UUID id, String fieldName) {
		if (id == null) {
			throw new IllegalArgumentException(fieldName + "는 null일 수 없습니다.");
		}
	}

	private void validateTargetType(SettlementTargetType targetType) {
		if (targetType == null) {
			throw new IllegalArgumentException("정산 대상 타입은 null일 수 없습니다.");
		}
	}

	private void validateAmount(BigDecimal amount, String fieldName) {
		if (amount == null) {
			throw new IllegalArgumentException(fieldName + "은 null일 수 없습니다.");
		}
	}

	private void validateOccurredAt(LocalDateTime occurredAt) {
		if (occurredAt == null) {
			throw new IllegalArgumentException("발생 시각은 null일 수 없습니다.");
		}
	}

	private void validateReferenceIds(SettlementTargetType targetType, UUID paymentId, UUID refundId) {
		if (targetType == SettlementTargetType.PAYMENT && paymentId == null) {
			throw new IllegalArgumentException("결제 정산 대상은 결제 ID가 필요합니다.");
		}
		if (targetType == SettlementTargetType.REFUND && refundId == null) {
			throw new IllegalArgumentException("환불 정산 대상은 환불 ID가 필요합니다.");
		}
	}
}
