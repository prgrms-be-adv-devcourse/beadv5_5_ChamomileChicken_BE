package jabaclass.admin.product.application.service;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import jabaclass.admin.common.error.AdminErrorCode;
import jabaclass.admin.common.error.BusinessException;
import jabaclass.admin.product.application.usecase.ProductAdminUseCase;
import jabaclass.admin.product.domain.model.OutboxEvent;
import jabaclass.admin.product.domain.repository.OutboxEventRepository;
import jabaclass.admin.product.domain.repository.ProductAdminRepository;
import jabaclass.admin.product.infrastructure.kafka.AdminProductEvent;
import jabaclass.admin.product.infrastructure.outbox.EventType;
import jabaclass.admin.product.presentation.dto.response.ProductAdminResponseDto;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProductAdminService implements ProductAdminUseCase {

	private final ProductAdminRepository productAdminRepository;
	private final OutboxEventRepository outboxEventRepository;
	private final ObjectMapper objectMapper;

	@Override
	@Transactional(readOnly = true, transactionManager = "productTransactionManager")
	public Page<ProductAdminResponseDto> getProducts(Pageable pageable) {
		return productAdminRepository.findAll(pageable)
			.map(ProductAdminResponseDto::from);
	}

	@Override
	@Transactional(transactionManager = "productTransactionManager")
	public void forceDownProduct(UUID productId) {
		productAdminRepository.findById(productId)
			.orElseThrow(() -> new BusinessException(AdminErrorCode.PRODUCT_NOT_FOUND))
			.forceDown();

		try {
			String payload = objectMapper.writeValueAsString(AdminProductEvent.forceDown(productId));
			outboxEventRepository.save(OutboxEvent.create(
				"product",
				productId.toString(),
				EventType.PRODUCT_FORCE_DOWN,
				payload
			));
		} catch (JsonProcessingException e) {
			throw new RuntimeException("OutboxEvent 직렬화 실패. productId=" + productId, e);
		}
	}
}
