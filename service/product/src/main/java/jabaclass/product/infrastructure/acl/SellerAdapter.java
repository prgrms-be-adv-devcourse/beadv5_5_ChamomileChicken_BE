package jabaclass.product.infrastructure.acl;

import java.util.Optional;
import java.util.UUID;

import jabaclass.product.application.acl.SellerReository;
import jabaclass.product.infrastructure.acl.client.SellerClient;
import jabaclass.product.infrastructure.acl.dto.SellerResponseDto;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class SellerAdapter implements SellerReository {

	private final SellerClient sellerClient;

	@Override
	public Optional<SellerResponseDto> findSeller(UUID sellerId) {
		return sellerClient.findSeller(sellerId);
	}
}
