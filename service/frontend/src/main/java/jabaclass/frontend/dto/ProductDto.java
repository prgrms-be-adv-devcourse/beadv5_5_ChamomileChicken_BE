package jabaclass.frontend.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
public class ProductDto {
    private UUID id;
    private String sellerName;
    private String title;
    private int maxCapacity;
    private String description;
    private String descriptionImage;
    private BigDecimal price;
    private String statusName;
}
