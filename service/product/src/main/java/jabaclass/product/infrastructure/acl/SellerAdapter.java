package jabaclass.product.infrastructure.acl;

import java.util.UUID;

import jabaclass.product.application.acl.SellerReository;
import jabaclass.product.application.exception.BusinessException;
import jabaclass.product.common.exception.CommonErrorCode;
import jabaclass.product.infrastructure.acl.client.SellerClient;
import jabaclass.product.infrastructure.acl.dto.SellerResposeDto;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class SellerAdapter implements SellerReository {

	private final SellerClient sellerClient;
	
	@Override
	public SellerResposeDto loadSellerId(UUID sellerId) {
		return sellerClient.findSeller(sellerId)
			.orElseThrow(() -> new BusinessException(
				CommonErrorCode.SELLER_NOT_FOUND
			));
	}
}
