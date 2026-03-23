package jabaclass.user.deposit.infrastructure.client;

import java.math.BigDecimal;
import java.util.UUID;

public interface PaymentClient {

	UUID createPayment(UUID userId, BigDecimal amount, String paymentMethod);

	UUID prepareDepositPayment(UUID userId, BigDecimal amount, String paymentMethod);

	UUID confirmDepositPayment(UUID prepareId);
}
