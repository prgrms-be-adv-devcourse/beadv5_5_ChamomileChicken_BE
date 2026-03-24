package jabaclass.product.infrastructure.acl.dto;

import java.util.UUID;

public record SellerResposeDto(
	UUID sellerId,
	String sellerName,
	String role
) {

}
