package jabaclass.order.application.port.external;

import java.math.BigDecimal;
import java.util.UUID;

public interface PaymentPort {

    BigDecimal refund(UUID orderId, BigDecimal refundRate);
}