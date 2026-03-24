package jabaclass.product.application.acl;

import java.util.UUID;

import jabaclass.product.infrastructure.acl.dto.SellerResposeDto;

public interface SellerReository {

	SellerResposeDto loadSellerId(UUID sellerId);
}
