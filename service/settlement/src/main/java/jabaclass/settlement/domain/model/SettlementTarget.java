package jabaclass.settlement.domain.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;

@Getter
@Entity
@Table(name = "settlement_targets")
public class SettlementTarget extends BaseEntity {

	/**
	 * 형식: yyyy-MM
	 */
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

	@Column(name = "product_schedule_id", nullable = false)
	private UUID productScheduleId;

	@Column(name = "buyer_id", nullable = false)
	private UUID buyerId;

	@Column(name = "participant_user_id")
	private UUID participantUserId;

	@Enumerated(EnumType.STRING)
	@Column(name = "target_type", nullable = false, length = 20)
	private SettlementTargetType targetType;

	/**
	 * 원천 데이터 상태 저장
	 * PAYMENT -> PAID
	 * REFUND -> COMPLETED
	 */
	@Column(name = "source_status", nullable = false, length = 30)
	private String sourceStatus;

	@Column(name = "quantity", nullable = false)
	private Integer quantity;

	@Column(name = "unit_price", nullable = false, precision = 19, scale = 2)
	private BigDecimal unitPrice;

	@Column(name = "gross_amount", nullable = false, precision = 19, scale = 2)
	private BigDecimal grossAmount;

	/**
	 * 정산 반영 금액
	 * 결제면 양수
	 * 환불이면 음수
	 */
	@Column(name = "settlement_amount", nullable = false, precision = 19, scale = 2)
	private BigDecimal settlementAmount;

	@Column(name = "occurred_at", nullable = false)
	private LocalDateTime occurredAt;

	protected SettlementTarget() {
	}

	public SettlementTarget(
		String settlementMonth,
		UUID sellerId,
		UUID orderId,
		UUID paymentId,
		UUID refundId,
		UUID productId,
		UUID productScheduleId,
		UUID buyerId,
		UUID participantUserId,
		SettlementTargetType targetType,
		String sourceStatus,
		Integer quantity,
		BigDecimal unitPrice,
		BigDecimal grossAmount,
		BigDecimal settlementAmount,
		LocalDateTime occurredAt
	) {
		validateSettlementMonth(settlementMonth);
		validateSellerId(sellerId);
		validateOrderId(orderId);
		validateProductId(productId);
		validateProductScheduleId(productScheduleId);
		validateBuyerId(buyerId);
		validateTargetType(targetType);
		validateSourceStatus(sourceStatus);
		validateQuantity(quantity);
		validateAmount(unitPrice, "단가");
		validateAmount(grossAmount, "총 주문 금액");
		validateAmount(settlementAmount, "정산 반영 금액");
		validateOccurredAt(occurredAt);
		validateReferenceIds(targetType, paymentId, refundId);

		this.settlementMonth = settlementMonth;
		this.sellerId = sellerId;
		this.orderId = orderId;
		this.paymentId = paymentId;
		this.refundId = refundId;
		this.productId = productId;
		this.productScheduleId = productScheduleId;
		this.buyerId = buyerId;
		this.participantUserId = participantUserId;
		this.targetType = targetType;
		this.sourceStatus = sourceStatus;
		this.quantity = quantity;
		this.unitPrice = unitPrice;
		this.grossAmount = grossAmount;
		this.settlementAmount = settlementAmount;
		this.occurredAt = occurredAt;
	}

	public static SettlementTarget forPayment(
		String settlementMonth,
		UUID sellerId,
		UUID orderId,
		UUID paymentId,
		UUID productId,
		UUID productScheduleId,
		UUID buyerId,
		UUID participantUserId,
		Integer quantity,
		BigDecimal unitPrice,
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
			productScheduleId,
			buyerId,
			participantUserId,
			SettlementTargetType.PAYMENT,
			"PAID",
			quantity,
			unitPrice,
			grossAmount,
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
		UUID productScheduleId,
		UUID buyerId,
		UUID participantUserId,
		Integer quantity,
		BigDecimal unitPrice,
		BigDecimal grossAmount,
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
			productScheduleId,
			buyerId,
			participantUserId,
			SettlementTargetType.REFUND,
			"COMPLETED",
			quantity,
			unitPrice,
			grossAmount,
			refundAmount.negate(),
			occurredAt
		);
	}

	private void validateSettlementMonth(String settlementMonth) {
		if (settlementMonth == null || settlementMonth.isBlank()) {
			throw new IllegalArgumentException("정산월은 비어 있을 수 없습니다.");
		}
	}

	private void validateSellerId(UUID sellerId) {
		if (sellerId == null) {
			throw new IllegalArgumentException("판매자 ID는 null일 수 없습니다.");
		}
	}

	private void validateOrderId(UUID orderId) {
		if (orderId == null) {
			throw new IllegalArgumentException("주문 ID는 null일 수 없습니다.");
		}
	}

	private void validateProductId(UUID productId) {
		if (productId == null) {
			throw new IllegalArgumentException("상품 ID는 null일 수 없습니다.");
		}
	}

	private void validateProductScheduleId(UUID productScheduleId) {
		if (productScheduleId == null) {
			throw new IllegalArgumentException("상품 일정 ID는 null일 수 없습니다.");
		}
	}

	private void validateBuyerId(UUID buyerId) {
		if (buyerId == null) {
			throw new IllegalArgumentException("구매자 ID는 null일 수 없습니다.");
		}
	}

	private void validateTargetType(SettlementTargetType targetType) {
		if (targetType == null) {
			throw new IllegalArgumentException("정산 대상 타입은 null일 수 없습니다.");
		}
	}

	private void validateSourceStatus(String sourceStatus) {
		if (sourceStatus == null || sourceStatus.isBlank()) {
			throw new IllegalArgumentException("원천 상태는 비어 있을 수 없습니다.");
		}
	}

	private void validateQuantity(Integer quantity) {
		if (quantity == null || quantity <= 0) {
			throw new IllegalArgumentException("수량은 1 이상이어야 합니다.");
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

	private void validateReferenceIds(
		SettlementTargetType targetType,
		UUID paymentId,
		UUID refundId
	) {
		if (targetType == SettlementTargetType.PAYMENT && paymentId == null) {
			throw new IllegalArgumentException("결제 정산 대상은 paymentId가 필요합니다.");
		}

		if (targetType == SettlementTargetType.REFUND) {
			if (paymentId == null) {
				throw new IllegalArgumentException("환불 정산 대상은 paymentId가 필요합니다.");
			}
			if (refundId == null) {
				throw new IllegalArgumentException("환불 정산 대상은 refundId가 필요합니다.");
			}
		}
	}

	public String getSettlementMonth() {
		return settlementMonth;
	}

	public UUID getSellerId() {
		return sellerId;
	}

	public UUID getOrderId() {
		return orderId;
	}

	public UUID getPaymentId() {
		return paymentId;
	}

	public UUID getRefundId() {
		return refundId;
	}

	public UUID getProductId() {
		return productId;
	}

	public UUID getProductScheduleId() {
		return productScheduleId;
	}

	public UUID getBuyerId() {
		return buyerId;
	}

	public UUID getParticipantUserId() {
		return participantUserId;
	}

	public SettlementTargetType getTargetType() {
		return targetType;
	}

	public String getSourceStatus() {
		return sourceStatus;
	}

	public Integer getQuantity() {
		return quantity;
	}

	public BigDecimal getUnitPrice() {
		return unitPrice;
	}

	public BigDecimal getGrossAmount() {
		return grossAmount;
	}

	public BigDecimal getSettlementAmount() {
		return settlementAmount;
	}

	public LocalDateTime getOccurredAt() {
		return occurredAt;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof SettlementTarget that)) return false;
		return Objects.equals(getId(), that.getId());
	}

	@Override
	public int hashCode() {
		return Objects.hash(getId());
	}
}
