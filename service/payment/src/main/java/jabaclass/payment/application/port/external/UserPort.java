package jabaclass.payment.application.port.external;

import java.math.BigDecimal;
import java.util.UUID;

public interface UserPort {

	void increaseDeposit(UUID userId, BigDecimal amount, UUID paymentId);
}