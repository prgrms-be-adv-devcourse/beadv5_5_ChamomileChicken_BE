package jabaclass.user.user.domain.model;

import jabaclass.user.common.model.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.UUID;

@Getter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Entity
@Table(name = "seller_settlement_accounts")
public class SellerSettlementAccount extends BaseEntity {

	@Column(name = "user_id", nullable = false, unique = true)
	private UUID userId;

	@Column(name = "bank_code", nullable = false, length = 20)
	private String bankCode;

	@Column(name = "account_number", nullable = false, length = 50)
	private String accountNumber;

	@Column(name = "account_holder", nullable = false, length = 100)
	private String accountHolder;

	@Column(name = "active", nullable = false)
	private boolean active;
}
