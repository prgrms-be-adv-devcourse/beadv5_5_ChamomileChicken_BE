package jabaclass.admin.settlement.domain.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import jabaclass.admin.common.model.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Getter
@SuperBuilder
@NoArgsConstructor
@Entity
@Table(name = "settlements")
public class Settlement extends BaseEntity {

	@Column(name = "seller_id", nullable = false)
	private UUID sellerId;

	@Column(name = "settlement_month", nullable = false, length = 7)
	private String settlementMonth;

	@Column(name = "original_amount", nullable = false, precision = 19, scale = 2)
	private BigDecimal originalAmount;

	@Column(name = "fee_amount", nullable = false, precision = 19, scale = 2)
	private BigDecimal feeAmount;

	@Column(name = "fee_rate", nullable = false, precision = 10, scale = 4)
	private BigDecimal feeRate;

	@Column(name = "settlement_amount", nullable = false, precision = 19, scale = 2)
	private BigDecimal settlementAmount;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 20)
	private SettlementStatus status;

	@Column(name = "transferred_at")
	private LocalDateTime transferredAt;

	@Column(name = "fail_reason", length = 500)
	private String failReason;
}
