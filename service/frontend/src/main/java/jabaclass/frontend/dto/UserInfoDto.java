package jabaclass.frontend.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
public class UserInfoDto {
    private UUID userId;
    private String name;
    private String email;
    private String phone;
    private BigDecimal deposit;
}
