package jabaclass.admin.product.application.usecase;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import jabaclass.admin.product.domain.dto.ProductSearchCondition;
import jabaclass.admin.product.presentation.dto.response.ProductAdminResponseDto;

public interface ProductAdminUseCase {

	Page<ProductAdminResponseDto> getProducts(Pageable pageable, ProductSearchCondition condition);

	void forceDownProduct(UUID productId);
}
