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

	public static SellerSettlementAccount register(
		UUID userId,
		String bankCode,
		String accountNumber,
		String accountHolder,
		boolean active
	) {
		validateUserId(userId);
		validateRequiredText(bankCode, "은행 코드");
		validateRequiredText(accountNumber, "계좌번호");
		validateRequiredText(accountHolder, "예금주");

		return SellerSettlementAccount.builder()
			.userId(userId)
			.bankCode(bankCode)
			.accountNumber(accountNumber)
			.accountHolder(accountHolder)
			.active(active)
			.build();
	}

	public void updateAccount(
		String bankCode,
		String accountNumber,
		String accountHolder,
		boolean active
	) {
		validateRequiredText(bankCode, "은행 코드");
		validateRequiredText(accountNumber, "계좌번호");
		validateRequiredText(accountHolder, "예금주");

		this.bankCode = bankCode;
		this.accountNumber = accountNumber;
		this.accountHolder = accountHolder;
		this.active = active;
	}

	private static void validateUserId(UUID userId) {
		if (userId == null) {
			throw new IllegalArgumentException("사용자 ID는 null일 수 없습니다.");
		}
	}

	private static void validateRequiredText(String value, String fieldName) {
		if (value == null || value.isBlank()) {
			throw new IllegalArgumentException(fieldName + "는 비어 있을 수 없습니다.");
		}
	}
}
