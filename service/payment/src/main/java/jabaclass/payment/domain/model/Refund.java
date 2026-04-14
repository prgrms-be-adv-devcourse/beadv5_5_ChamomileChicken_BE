package jabaclass.payment.domain.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;

@Entity
@Table(name = "refunds")
@Getter
public class Refund {

	@Id
	@Column(name = "id", nullable = false, updatable = false)
	private UUID id;

	@Column(name = "payment_id", nullable = false)
	private UUID paymentId;

	@Column(name = "deposit_payment_id")
	private UUID depositPaymentId;

	@Column(name = "payment_refund_amount", precision = 19, scale = 2)
	private BigDecimal paymentRefundAmount;

	@Column(name = "deposit_refund_amount", precision = 19, scale = 2)
	private BigDecimal depositRefundAmount;

	@Column(name = "total_refund_amount", precision = 19, scale = 2)
	private BigDecimal totalRefundAmount;

	@Column(name = "requested_at")
	private LocalDateTime requestedAt;

	@Column(name = "processed_at")
	private LocalDateTime processedAt;

	@Enumerated(EnumType.STRING)
	@Column(name = "refund_status", length = 200)
	private RefundStatus status;

	protected Refund() {
	}

	private Refund(
		UUID id,
		UUID paymentId,
		UUID depositPaymentId,
		BigDecimal paymentRefundAmount,
		BigDecimal depositRefundAmount,
		BigDecimal totalRefundAmount,
		LocalDateTime requestedAt,
		LocalDateTime processedAt,
		RefundStatus status
	) {
		this.id = id;
		this.paymentId = paymentId;
		this.depositPaymentId = depositPaymentId;
		this.paymentRefundAmount = paymentRefundAmount;
		this.depositRefundAmount = depositRefundAmount;
		this.totalRefundAmount = totalRefundAmount;
		this.requestedAt = requestedAt;
		this.processedAt = processedAt;
		this.status = status;
	}

	public static Refund create(
		UUID paymentId,
		UUID depositPaymentId,
		BigDecimal paymentRefundAmount,
		BigDecimal depositRefundAmount
	) {
		BigDecimal totalRefundAmount = paymentRefundAmount.add(depositRefundAmount);

		return new Refund(
			UUID.randomUUID(),
			paymentId,
			depositPaymentId,
			paymentRefundAmount,
			depositRefundAmount,
			totalRefundAmount,
			LocalDateTime.now(),
			null,
			RefundStatus.REQUESTED
		);
	}

	public void markProcessing() {
		this.status = RefundStatus.PROCESSING;
	}

	public void markCompleted() {
		this.status = RefundStatus.COMPLETED;
		this.processedAt = LocalDateTime.now();
	}

	public void markFailed() {
		this.status = RefundStatus.FAILED;
		this.processedAt = LocalDateTime.now();
	}
}
