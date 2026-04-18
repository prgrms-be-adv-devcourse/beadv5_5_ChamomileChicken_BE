package jabaclass.product.infrastructure.kafka.admin;

import java.util.UUID;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import jabaclass.product.application.exception.BusinessException;
import jabaclass.product.common.exception.CommonErrorCode;
import jabaclass.product.domain.model.Product;
import jabaclass.product.domain.model.status.ProductStatus;
import jabaclass.product.domain.repository.ProductRepository;
import jabaclass.product.domain.repository.ScheduleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class AdminProductEventHandler {

	private final ProductRepository productRepository;
	private final ScheduleRepository scheduleRepository;

	@Transactional
	public void processForceDown(UUID productId) {
		Product product = productRepository.findById(productId)
			.orElseThrow(() -> new BusinessException(CommonErrorCode.PRODUCT_NOT_FOUND));
		product.changeStatus(ProductStatus.DISABLE);
		product.changeDelete();
		int deletedCount = scheduleRepository.softDeleteByProductId(productId);
		log.info("FORCE_DOWN DB 처리 완료. productId={}, 삭제된 스케줄 수={}", productId, deletedCount);
	}

	@Transactional
	public void restoreProductStatus(UUID productId) {
		productRepository.findById(productId).ifPresent(p -> {
			p.changeStatus(ProductStatus.ENABLE);
			p.restoreDelete();
		});
		scheduleRepository.restoreDeleteByProductId(productId);
		log.info("FORCE_DOWN 보상 트랜잭션 완료 — product/schedule 복구. productId={}", productId);
	}
}
