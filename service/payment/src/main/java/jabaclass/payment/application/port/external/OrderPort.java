package jabaclass.payment.application.port.external;

import java.util.UUID;

public interface OrderPort {

	boolean validateOrder(UUID orderId, int amount);

	void updatePaymentStatus(UUID orderId, UUID paymentId, int depositAmount, String status);
}