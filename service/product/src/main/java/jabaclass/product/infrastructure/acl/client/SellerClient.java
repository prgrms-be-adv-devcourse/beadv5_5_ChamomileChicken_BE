package jabaclass.product.infrastructure.acl.client;

import java.util.Optional;
import java.util.UUID;

import jabaclass.product.infrastructure.acl.dto.SellerResponseDto;

public interface SellerClient {
	Optional<SellerResponseDto> findSeller(UUID sellerId);
}
