package jabaclass.payment.domain.model;

import jabaclass.payment.common.error.PaymentErrorCode;
import jabaclass.payment.common.error.PaymentException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "payments")
@Getter
public class Payment extends BaseEntity{

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@Column(name = "id", nullable = false, updatable = false)
	private UUID id;

	@Column(name = "user_id", nullable = false)
	private UUID userId;

	@Column(name = "product_id", nullable = false)
	private UUID productId;

	@Column(name = "order_id", nullable = false)
	private UUID orderId;

	@Enumerated(EnumType.STRING)
	@Column(name = "payment_method", length = 20)
	private PaymentMethod paymentMethod;

	@Column(name = "payment_amount", precision = 19, scale = 2)
	private BigDecimal paymentAmount;

	@Column(name = "deposit_amount", precision = 19, scale = 2)
	private BigDecimal depositAmount;

	@Column(name = "total_amount", nullable = false, precision = 19, scale = 2)
	private BigDecimal totalAmount;

	@Enumerated(EnumType.STRING)
	@Column(name = "payment_status", nullable = false, length = 20)
	private PaymentStatus status;

	@Column(name = "payment_key")
	private String paymentKey;

	@Column(name = "paid_at")
	private LocalDateTime paidAt;

	protected Payment() {

	}

	private Payment(
		UUID id,
		UUID userId,
		UUID productId,
		UUID orderId,
		PaymentMethod paymentMethod,
		BigDecimal paymentAmount,
		BigDecimal depositAmount,
		BigDecimal totalAmount,
		PaymentStatus status
	) {
		this.id = id;
		this.userId = userId;
		this.productId = productId;
		this.orderId = orderId;
		this.paymentMethod = paymentMethod;
		this.paymentAmount = paymentAmount;
		this.depositAmount = depositAmount;
		this.totalAmount = totalAmount;
		this.status = status;
	}

	public static Payment create(
		UUID userId,
		UUID productId,
		UUID orderId,
		PaymentMethod paymentMethod,
		BigDecimal paymentAmount,
		BigDecimal depositAmount
	) {
		validate(paymentAmount, depositAmount);

		BigDecimal totalAmount = paymentAmount.add(depositAmount);

		return new Payment(
			null,
			userId,
			productId,
			orderId,
			paymentMethod,
			paymentAmount,
			depositAmount,
			totalAmount,
			PaymentStatus.READY
		);
	}

	private static void validate(BigDecimal paymentAmount, BigDecimal depositAmount) {
		if (paymentAmount == null || depositAmount == null) {
			throw new PaymentException(PaymentErrorCode.INVALID_AMOUNT);
		}

		if (paymentAmount.compareTo(BigDecimal.ZERO) < 0) {
			throw new PaymentException(PaymentErrorCode.INVALID_NEGATIVE_AMOUNT);
		}

		if (depositAmount.compareTo(BigDecimal.ZERO) < 0) {
			throw new PaymentException(PaymentErrorCode.INVALID_NEGATIVE_AMOUNT);
		}
	}

	public boolean isDone() {
		return this.status == PaymentStatus.PAID;
	}

	public void markDone(String paymentKey) {
		this.paymentKey = paymentKey;
		this.status = PaymentStatus.PAID;
		this.paidAt = LocalDateTime.now();
	}

	public void markFailed() {
		if (this.status == PaymentStatus.PAID) {
			throw new PaymentException(PaymentErrorCode.PAYMENT_ALREADY_COMPLETED);
		}

		this.status = PaymentStatus.FAILED;
	}

	public void markCancelled() {
		if (this.status != PaymentStatus.PAID) {
			throw new PaymentException(PaymentErrorCode.PAYMENT_NOT_COMPLETED);
		}

		this.status = PaymentStatus.CANCELLED;
	}

	public void expire() {
		if (this.status != PaymentStatus.READY) {
			throw new PaymentException(PaymentErrorCode.PAYMENT_INVALID_STATUS);
		}
		this.status = PaymentStatus.EXPIRED;
	}
}
