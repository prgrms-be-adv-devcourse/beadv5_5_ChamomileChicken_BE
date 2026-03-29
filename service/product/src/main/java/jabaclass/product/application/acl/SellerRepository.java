package jabaclass.product.application.acl;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import jabaclass.product.infrastructure.acl.dto.SellerResponseDto;

public interface SellerRepository {
	Optional<SellerResponseDto> findSeller(UUID sellerId);

	Optional<List<SellerResponseDto>> findSellerList(List<UUID> sellerIds);
}
