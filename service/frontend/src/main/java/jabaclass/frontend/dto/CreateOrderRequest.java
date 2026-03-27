package jabaclass.frontend.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
public class CreateOrderRequest {
    private UUID productId;
    private UUID productScheduleId;
    private Integer quantity;
    private BigDecimal depositAmount;
}