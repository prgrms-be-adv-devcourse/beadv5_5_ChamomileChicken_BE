package jabaclass.payment.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "payments")
@Getter
public class Payment {

	@Id
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
		validateAmount(paymentAmount, depositAmount);

		BigDecimal totalAmount = paymentAmount.add(depositAmount);

		return new Payment(
			UUID.randomUUID(),
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

	private static void validateAmount(
		BigDecimal paymentAmount,
		BigDecimal depositAmount
	) {
		if (paymentAmount == null || depositAmount == null) {
			throw new IllegalArgumentException("금액은 null일 수 없습니다");
		}

		if (paymentAmount.compareTo(BigDecimal.ZERO) < 0) {
			throw new IllegalArgumentException("paymentAmount는 0 이상이어야 합니다");
		}

		if (depositAmount.compareTo(BigDecimal.ZERO) < 0) {
			throw new IllegalArgumentException("depositAmount는 0 이상이어야 합니다");
		}
	}

	public boolean isDone() {
		return this.status == PaymentStatus.PAID;
	}

	public void markDone(String paymentKey) {
		this.paymentKey = paymentKey;
		this.status = PaymentStatus.PAID;
	}

	public void markFailed() {
		if (this.status == PaymentStatus.PAID) {
			throw new IllegalStateException("이미 완료된 결제는 실패 처리할 수 없습니다.");
		}

		this.status = PaymentStatus.FAILED;
	}
	public void markCancelled() {
		if (this.status == PaymentStatus.PAID) {
			throw new IllegalStateException("완료된 결제는 취소할 수 없습니다.");
		}

		this.status = PaymentStatus.CANCELLED;
	}
}
