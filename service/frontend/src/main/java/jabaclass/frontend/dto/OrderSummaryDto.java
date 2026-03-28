package jabaclass.frontend.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
public class OrderSummaryDto {
    private UUID id;
    private UUID productScheduleId;
    private Integer quantity;
    private BigDecimal totalAmount;
    private String status;
}