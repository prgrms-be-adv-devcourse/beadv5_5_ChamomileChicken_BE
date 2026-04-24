package jabaclass.admin.product.domain.dto;

import java.util.UUID;

public record ProductSearchCondition(
	String status,
	UUID sellerId,
	String title
) {
}