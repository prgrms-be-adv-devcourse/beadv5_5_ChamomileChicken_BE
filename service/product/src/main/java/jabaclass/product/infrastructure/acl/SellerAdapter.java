package jabaclass.product.infrastructure.acl;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Component;

import jabaclass.product.application.acl.SellerRepository;
import jabaclass.product.infrastructure.acl.client.SellerClient;
import jabaclass.product.infrastructure.acl.dto.SellerResponseDto;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Component
public class SellerAdapter implements SellerRepository {

	private final SellerClient sellerClient;

	@Override
	public Optional<SellerResponseDto> findSeller(UUID sellerId) {
		return sellerClient.findSeller(sellerId);
	}

	@Override
	public Optional<List<SellerResponseDto>> findSellerList(List<UUID> sellerIds) {

		return sellerClient.findSellerList(sellerIds);
	}
}
