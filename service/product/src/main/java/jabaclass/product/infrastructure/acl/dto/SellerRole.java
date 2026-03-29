package jabaclass.product.infrastructure.acl.dto;

import jabaclass.product.application.exception.BusinessException;
import jabaclass.product.common.exception.CommonErrorCode;

public enum SellerRole {
	SELLER,
	USER;

	public static SellerRole from(String role) {
		try {
			return SellerRole.valueOf(role);
		} catch (Exception e) {
			throw new BusinessException(CommonErrorCode.INVALID_ROLE);
		}
	}
}
