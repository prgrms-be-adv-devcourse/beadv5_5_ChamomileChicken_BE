package jabaclass.product.infrastructure.acl.client;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import jabaclass.product.infrastructure.acl.dto.response.UserResponseDto;

public interface SellerClient {
	Optional<UserResponseDto> findSeller(UUID sellerId);

	Optional<List<UserResponseDto>> findSellerList(List<UUID> sellerIds);
}
