package jabaclass.payment.application.port.external;

import java.util.UUID;

public interface OrderPort {

	boolean validateOrder(UUID orderId, int amount);
}
