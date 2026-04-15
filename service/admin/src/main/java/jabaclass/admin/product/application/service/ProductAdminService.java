package jabaclass.admin.product.application.service;

import java.util.UUID;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jabaclass.admin.common.error.AdminErrorCode;
import jabaclass.admin.common.error.BusinessException;
import jabaclass.admin.product.application.usecase.ProductAdminUseCase;
import jabaclass.admin.product.domain.repository.ProductAdminRepository;
import jabaclass.admin.product.infrastructure.kafka.AdminProductEvent;
import jabaclass.admin.product.presentation.dto.response.ProductAdminResponseDto;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProductAdminService implements ProductAdminUseCase {

	private final ProductAdminRepository productAdminRepository;
	private final ApplicationEventPublisher eventPublisher;

	@Override
	@Transactional(readOnly = true, transactionManager = "productTransactionManager")
	public Page<ProductAdminResponseDto> getProducts(Pageable pageable) {
		return productAdminRepository.findAll(pageable)
			.map(ProductAdminResponseDto::from);
	}

	// 상품 삭제
	@Override
	@Transactional(transactionManager = "productTransactionManager")
	public void forceDownProduct(UUID productId) {
		productAdminRepository.findById(productId)
			.orElseThrow(() -> new BusinessException(AdminErrorCode.PRODUCT_NOT_FOUND))
			.forceDown();

		eventPublisher.publishEvent(AdminProductEvent.forceDown(productId));
	}
}
