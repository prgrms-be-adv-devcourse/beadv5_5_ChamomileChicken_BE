package jabaclass.order.application.port.external;

import java.util.UUID;
import java.math.BigDecimal;

public interface DepositPort {

    boolean validateAndUse(UUID userId, BigDecimal depositAmount);
}
