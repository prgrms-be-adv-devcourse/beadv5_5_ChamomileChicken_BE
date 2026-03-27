package jabaclass.frontend.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
public class CreateOrderResponse {
    private UUID id;
    private UUID buyerId;
    private UUID productId;
    private UUID productScheduleId;
    private Integer quantity;
    private BigDecimal totalAmount;
    private BigDecimal depositAmount;
    private BigDecimal paymentAmount;
    private String status;
}