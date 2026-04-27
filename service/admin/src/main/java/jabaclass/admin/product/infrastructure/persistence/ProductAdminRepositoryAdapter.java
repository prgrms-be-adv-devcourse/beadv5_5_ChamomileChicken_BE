package jabaclass.admin.product.infrastructure.persistence;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import jabaclass.admin.product.domain.dto.ProductSearchCondition;
import jabaclass.admin.product.domain.model.Product;
import jabaclass.admin.product.domain.model.ProductStatus;
import jabaclass.admin.product.domain.repository.ProductAdminRepository;
import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class ProductAdminRepositoryAdapter implements ProductAdminRepository {

	private final ProductAdminJpaRepository productAdminJpaRepository;

	@Override
	public Page<Product> findAll(Pageable pageable) {
		return productAdminJpaRepository.findAll(pageable);
	}

	@Override
	public Page<Product> findAll(ProductSearchCondition condition, Pageable pageable) {
		return productAdminJpaRepository.findAll(ProductAdminSpecification.withCondition(condition), pageable);
	}

	@Override
	public Optional<Product> findById(UUID productId) {
		return productAdminJpaRepository.findById(productId);
	}

	@Override
	public long countActiveProducts() {
		return productAdminJpaRepository.countByStatusAndDeleteDtIsNull(ProductStatus.ENABLE);
	}
}
