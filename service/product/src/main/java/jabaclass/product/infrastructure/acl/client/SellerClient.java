package jabaclass.product.infrastructure.acl.client;

import java.util.Optional;
import java.util.UUID;

import jabaclass.product.infrastructure.acl.dto.SellerResposeDto;

public interface SellerClient {
	Optional<SellerResposeDto> findSeller(UUID sellerId);
}
