package jabaclass.settlement.domain.model.settlement;

import java.math.BigDecimal;
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

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "settlements_history")
public class SettlementHistory extends BaseEntity {

	@Column(name = "settlement_id", nullable = false)
	private UUID settlementId;

	@Column(name = "settlement_target_id", nullable = false)
	private UUID settlementTargetId;

	@Column(name = "seller_id", nullable = false)
	private UUID sellerId;

	@Column(name = "product_id", nullable = false)
	private UUID productId;

	/**
	 * 형식: yyyy-MM
	 */
	@Column(name = "settlement_month", nullable = false, length = 7)
	private String settlementMonth;

	@Column(name = "original_amount", nullable = false, precision = 19, scale = 2)
	private BigDecimal originalAmount;

	@Column(name = "fee_amount", nullable = false, precision = 19, scale = 2)
	private BigDecimal feeAmount;

	@Column(name = "settlement_amount", nullable = false, precision = 19, scale = 2)
	private BigDecimal settlementAmount;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 20)
	private SettlementStatus status;

	public SettlementHistory(
		UUID settlementId,
		UUID settlementTargetId,
		UUID sellerId,
		UUID productId,
		String settlementMonth,
		BigDecimal originalAmount,
		BigDecimal feeAmount,
		BigDecimal settlementAmount,
		SettlementStatus status
	) {
		validateSettlementId(settlementId);
		validateSettlementTargetId(settlementTargetId);
		validateSellerId(sellerId);
		validateProductId(productId);
		validateSettlementMonth(settlementMonth);
		validateAmount(originalAmount, "정산 원금");
		validateAmount(feeAmount, "수수료");
		validateAmount(settlementAmount, "최종 정산금");
		validateStatus(status);

		this.settlementId = settlementId;
		this.settlementTargetId = settlementTargetId;
		this.sellerId = sellerId;
		this.productId = productId;
		this.settlementMonth = settlementMonth;
		this.originalAmount = originalAmount;
		this.feeAmount = feeAmount;
		this.settlementAmount = settlementAmount;
		this.status = status;
	}

	public static SettlementHistory create(
		UUID settlementId,
		UUID settlementTargetId,
		UUID sellerId,
		UUID productId,
		String settlementMonth,
		BigDecimal originalAmount,
		BigDecimal feeAmount,
		BigDecimal settlementAmount,
		SettlementStatus status
	) {
		return new SettlementHistory(
			settlementId,
			settlementTargetId,
			sellerId,
			productId,
			settlementMonth,
			originalAmount,
			feeAmount,
			settlementAmount,
			status
		);
	}

	private void validateSettlementId(UUID settlementId) {
		if (settlementId == null) {
			throw new IllegalArgumentException("정산 ID는 null일 수 없습니다.");
		}
	}

	private void validateSettlementTargetId(UUID settlementTargetId) {
		if (settlementTargetId == null) {
			throw new IllegalArgumentException("정산 대상 ID는 null일 수 없습니다.");
		}
	}

	private void validateSellerId(UUID sellerId) {
		if (sellerId == null) {
			throw new IllegalArgumentException("판매자 ID는 null일 수 없습니다.");
		}
	}

	private void validateProductId(UUID productId) {
		if (productId == null) {
			throw new IllegalArgumentException("상품 ID는 null일 수 없습니다.");
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

	private void validateStatus(SettlementStatus status) {
		if (status == null) {
			throw new IllegalArgumentException("정산 상태는 null일 수 없습니다.");
		}
	}
}
