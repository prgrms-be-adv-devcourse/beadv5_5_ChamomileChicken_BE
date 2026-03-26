package jabaclass.product.infrastructure.persistence;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import jabaclass.product.domain.model.Products;
import jabaclass.product.domain.model.status.ProductStatus;
import jabaclass.product.domain.repository.ProductRepository;
import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class ProductRepositoryAdapter implements ProductRepository {

	private final ProductJpaRepository productJpaRepository;

	@Override
	public Products save(Products product) {
		return productJpaRepository.save(product);
	}

	@Override
	public Optional<Products> findById(UUID id) {
		return productJpaRepository.findById(id);
	}

	@Override
	public Page<Products> findByStatusAndTitleContainingAndDeleteDtIsNull(ProductStatus status, String keyword,
		Pageable pageable) {
		return productJpaRepository.findByStatusAndTitleContainingAndDeleteDtIsNull(status, keyword,
			pageable);
	}

	@Override
	public Page<Products> findByStatusAndDeleteDtIsNull(ProductStatus status, Pageable pageable) {
		return productJpaRepository.findByStatusAndDeleteDtIsNull(status, pageable);
	}

	@Override
	public Optional<Products> findByIdAndSellerId(UUID productId, UUID sellerId) {
		return productJpaRepository.findByIdAndSellerId(productId, sellerId);
	}

}
