package jabaclass.frontend.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class CreateProductRequest {
    private UUID sellerId;
    private String title;
    private int maxCapacity;
    private String description;
    private List<String> imageIds;
    private BigDecimal price;
    private String status;
}
