package jabaclass.frontend.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@AllArgsConstructor
public class UpdatePaymentStatusRequest {
    private UUID paymentId;
    private String paymentStatus;
    private BigDecimal depositAmount;
}