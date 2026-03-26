package jabaclass.order.order.application.client;

import java.util.UUID;
import java.math.BigDecimal;

public interface DepositClient {

    boolean isValid(UUID userId, BigDecimal depositAmount);

    void use(UUID userId, BigDecimal amount);
}
