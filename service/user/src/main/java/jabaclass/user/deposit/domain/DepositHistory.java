package jabaclass.user.deposit.domain;

import java.math.BigDecimal;
import java.util.UUID;

import jabaclass.user.common.model.BaseEntity;
import jabaclass.user.user.domain.model.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;

@Getter
@Entity
@Table(name = "deposit_histories")
public class DepositHistory extends BaseEntity {

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@Column(nullable = false)
	private UUID paymentId;

	@Column(nullable = false, precision = 19, scale = 2)
	private BigDecimal amount;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 10)
	private DepositType type;

	protected DepositHistory() {
	}

	private DepositHistory(User user, UUID paymentId, BigDecimal amount, DepositType type) {
		this.user = user;
		this.paymentId = paymentId;
		this.amount = amount;
		this.type = type;
	}

	public static DepositHistory of(User user, UUID paymentId, BigDecimal amount, DepositType type) {
		return new DepositHistory(user, paymentId, amount, type);
	}
}

