package jabaclass.order.infrastructure.client.product.dto;

import java.time.LocalDate;

public record ProductScheduleDateResponseDto(
	LocalDate startDate
) {
}