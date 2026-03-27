package jabaclass.frontend.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
public class PreparePaymentRequest {
    private UUID userId;
    private UUID productId;
    private UUID orderId;
    private String paymentMethod;
    private BigDecimal paymentAmount;
    private BigDecimal depositAmount;
}