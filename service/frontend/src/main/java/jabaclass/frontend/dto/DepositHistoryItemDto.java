package jabaclass.frontend.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class DepositHistoryItemDto {
    private String type;
    private BigDecimal amount;
    private String createdAt;
}