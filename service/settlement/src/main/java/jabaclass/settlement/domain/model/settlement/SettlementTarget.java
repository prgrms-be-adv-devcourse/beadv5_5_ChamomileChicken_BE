package jabaclass.settlement.domain.model.settlement;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import jabaclass.settlement.domain.model.BaseEntity;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
	@Table(
	name = "settlement_targets",
	indexes = {
		@Index(
			name = "idx_settlement_targets_batch_cursor",
			columnList = "settlement_month, target_type, calculation_status, occurred_at, id"
		),
		@Index(
			name = "idx_settlement_targets_payment_type",
			columnList = "payment_id, target_type"
		)
	}
)
public class SettlementTarget extends BaseEntity {

	@Column(name = "source_event_id", nullable = false, unique = true, updatable = false)
	private UUID sourceEventId;

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

	@Column(name = "settlement_base_amount", nullable = false, precision = 19, scale = 2)
	private BigDecimal settlementBaseAmount;

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
		UUID sourceEventId,
		String settlementMonth,
		UUID sellerId,
		UUID orderId,
		UUID paymentId,
		UUID refundId,
		UUID productId,
		SettlementTargetType targetType,
		BigDecimal settlementBaseAmount,
		LocalDateTime occurredAt
	) {
		validateRequiredId(sourceEventId, "이벤트 ID");
		validateSettlementMonth(settlementMonth);
		validateRequiredId(sellerId, "판매자 ID");
		validateRequiredId(orderId, "주문 ID");
		validateRequiredId(productId, "상품 ID");
		validateTargetType(targetType);
		validateAmount(settlementBaseAmount, "정산 기준 금액");
		validateOccurredAt(occurredAt);
		validateReferenceIds(targetType, paymentId, refundId);

		this.sourceEventId = sourceEventId;
		this.settlementMonth = settlementMonth;
		this.sellerId = sellerId;
		this.orderId = orderId;
		this.paymentId = paymentId;
		this.refundId = refundId;
		this.productId = productId;
		this.targetType = targetType;
		this.settlementBaseAmount = settlementBaseAmount;
		this.occurredAt = occurredAt;
		this.calculationStatus = SettlementTargetCalculationStatus.PENDING;
		this.calculationRequestedAt = LocalDateTime.now();
	}

	public static SettlementTarget forPayment(
		UUID sourceEventId,
		String settlementMonth,
		UUID sellerId,
		UUID orderId,
		UUID paymentId,
		UUID productId,
		BigDecimal settlementBaseAmount,
		LocalDateTime occurredAt
	) {
		return new SettlementTarget(
			sourceEventId,
			settlementMonth,
			sellerId,
			orderId,
			paymentId,
			null,
			productId,
			SettlementTargetType.PAYMENT,
			settlementBaseAmount,
			occurredAt
		);
	}

	public static SettlementTarget forRefund(
		UUID sourceEventId,
		String settlementMonth,
		UUID sellerId,
		UUID orderId,
		UUID paymentId,
		UUID refundId,
		UUID productId,
		BigDecimal settlementBaseAmount,
		LocalDateTime occurredAt
	) {
		return new SettlementTarget(
			sourceEventId,
			settlementMonth,
			sellerId,
			orderId,
			paymentId,
			refundId,
			productId,
			SettlementTargetType.REFUND,
			settlementBaseAmount.abs().negate(),
			occurredAt
		);
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
