package jabaclass.settlement.application.port.external;

import java.time.LocalDateTime;
import java.util.UUID;

import jabaclass.settlement.application.dto.PaymentSettlementSource;
import jabaclass.settlement.application.dto.RefundSettlementSource;
import jabaclass.settlement.application.dto.SettlementSliceResult;

public interface PaymentSettlementPort {

	SettlementSliceResult<PaymentSettlementSource> fetchPayments(
		LocalDateTime from,
		LocalDateTime to,
		LocalDateTime cursorUpdatedAt,
		UUID cursorId,
		int size
	);

	SettlementSliceResult<RefundSettlementSource> fetchRefunds(
		LocalDateTime from,
		LocalDateTime to,
		LocalDateTime cursorUpdatedAt,
		UUID cursorId,
		int size
	);
}