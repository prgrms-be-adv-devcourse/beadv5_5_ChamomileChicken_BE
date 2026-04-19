package jabaclass.settlement.domain.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "settlement_transfers")
public class SettlementTransfer extends BaseEntity {

	@Column(name = "settlement_id", nullable = false)
	private UUID settlementId;

	@Enumerated(EnumType.STRING)
	@Column(name = "transfer_status", nullable = false, length = 20)
	private SettlementTransferStatus transferStatus;

	@Column(name = "bank_code", length = 20)
	private String bankCode;

	@Column(name = "account_number_masked", length = 100)
	private String accountNumberMasked;

	@Column(name = "amount", nullable = false, precision = 19, scale = 2)
	private BigDecimal amount;

	@Column(name = "requested_at", nullable = false)
	private LocalDateTime requestedAt;

	@Column(name = "completed_at")
	private LocalDateTime completedAt;

	@Column(name = "fail_reason", length = 500)
	private String failReason;

	private SettlementTransfer(
		UUID settlementId,
		SettlementTransferStatus transferStatus,
		String bankCode,
		String accountNumberMasked,
		BigDecimal amount,
		LocalDateTime requestedAt,
		LocalDateTime completedAt,
		String failReason
	) {
		validateSettlementId(settlementId);
		validateTransferStatus(transferStatus);
		validateAmount(amount);
		validateRequestedAt(requestedAt);

		this.settlementId = settlementId;
		this.transferStatus = transferStatus;
		this.bankCode = bankCode;
		this.accountNumberMasked = accountNumberMasked;
		this.amount = amount;
		this.requestedAt = requestedAt;
		this.completedAt = completedAt;
		this.failReason = failReason;
	}

	public static SettlementTransfer requested(
		UUID settlementId,
		String bankCode,
		String accountNumber,
		BigDecimal amount
	) {
		return new SettlementTransfer(
			settlementId,
			SettlementTransferStatus.REQUESTED,
			bankCode,
			maskAccountNumber(accountNumber),
			amount,
			LocalDateTime.now(),
			null,
			null
		);
	}

	public static SettlementTransfer hold(
		UUID settlementId,
		BigDecimal amount,
		String failReason
	) {
		return new SettlementTransfer(
			settlementId,
			SettlementTransferStatus.HOLD,
			null,
			null,
			amount,
			LocalDateTime.now(),
			null,
			failReason
		);
	}

	public void markSent() {
		this.transferStatus = SettlementTransferStatus.SENT;
		this.completedAt = LocalDateTime.now();
		this.failReason = null;
	}

	public void markFailed(String failReason) {
		this.transferStatus = SettlementTransferStatus.FAILED;
		this.completedAt = LocalDateTime.now();
		this.failReason = failReason;
	}

	private static String maskAccountNumber(String accountNumber) {
		if (accountNumber == null || accountNumber.isBlank()) {
			return null;
		}
		if (accountNumber.length() <= 4) {
			return "*".repeat(accountNumber.length());
		}
		return "*".repeat(accountNumber.length() - 4) + accountNumber.substring(accountNumber.length() - 4);
	}

	private void validateSettlementId(UUID settlementId) {
		if (settlementId == null) {
			throw new IllegalArgumentException("정산 ID는 null일 수 없습니다.");
		}
	}

	private void validateTransferStatus(SettlementTransferStatus transferStatus) {
		if (transferStatus == null) {
			throw new IllegalArgumentException("송금 상태는 null일 수 없습니다.");
		}
	}

	private void validateAmount(BigDecimal amount) {
		if (amount == null) {
			throw new IllegalArgumentException("송금 금액은 null일 수 없습니다.");
		}
	}

	private void validateRequestedAt(LocalDateTime requestedAt) {
		if (requestedAt == null) {
			throw new IllegalArgumentException("송금 요청 시각은 null일 수 없습니다.");
		}
	}
}
