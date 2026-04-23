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
import jabaclass.admin.product.domain.dto.ProductSearchCondition;
import jabaclass.admin.product.application.usecase.ProductAdminUseCase;
import jabaclass.admin.product.domain.model.OutboxEvent;
import jabaclass.admin.product.domain.repository.OutboxEventRepository;
import jabaclass.admin.product.domain.repository.ProductAdminRepository;
import jabaclass.admin.product.infrastructure.kafka.AdminProductEvent;
import jabaclass.admin.product.infrastructure.outbox.EventType;
import jabaclass.admin.product.presentation.dto.response.ProductAdminResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductAdminService implements ProductAdminUseCase {

	private final ProductAdminRepository productAdminRepository;
	private final OutboxEventRepository outboxEventRepository;
	private final ObjectMapper objectMapper;

	@Override
	@Transactional(readOnly = true)
	public Page<ProductAdminResponseDto> getProducts(Pageable pageable, ProductSearchCondition condition) {
		return productAdminRepository.findAll(condition, pageable)
			.map(ProductAdminResponseDto::from);
	}

	@Override
	@Transactional
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
			log.error("OutboxEvent 직렬화 실패. productId={}", productId, e);
			throw new BusinessException(AdminErrorCode.OUTBOX_SERIALIZATION_FAILED);
		}
	}
}
