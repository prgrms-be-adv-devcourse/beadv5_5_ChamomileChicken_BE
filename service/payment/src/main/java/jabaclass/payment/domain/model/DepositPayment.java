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
@Table(name = "deposit_payments")
@Getter
public class DepositPayment {

	@Id
	@Column(name = "id", nullable = false, updatable = false)
	private UUID id;

	@Column(name = "user_id", nullable = false)
	private UUID userId;

	@Column(name = "amount", nullable = false, precision = 19, scale = 2)
	private BigDecimal amount;

	@Column(name = "payment_method", length = 20)
	private String paymentMethod;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 20)
	private DepositPaymentStatus status;

	protected DepositPayment() {
	}

	private DepositPayment(UUID id, UUID userId, BigDecimal amount, String paymentMethod) {
		this.id = id;
		this.userId = userId;
		this.amount = amount;
		this.paymentMethod = paymentMethod;
		this.status = DepositPaymentStatus.READY;
	}

	public static DepositPayment create(UUID userId, BigDecimal amount, String paymentMethod) {
		return new DepositPayment(UUID.randomUUID(), userId, amount, paymentMethod);
	}

	public void markDone() {
		this.status = DepositPaymentStatus.DONE;
	}

	public void markFailed() {
		this.status = DepositPaymentStatus.FAILED;
	}

	public boolean isDone() {
		return this.status == DepositPaymentStatus.DONE;
	}
}
