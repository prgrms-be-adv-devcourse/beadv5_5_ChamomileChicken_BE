package jabaclass.product.infrastructure.persistence;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import jabaclass.product.domain.model.Product;
import jabaclass.product.domain.repository.ProductRepository;
import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class ProductRepositoryAdapter implements ProductRepository {

	private final ProductJpaRepository productJpaRepository;

	@Override
	public Product save(Product product) {
		return productJpaRepository.save(product);
	}

	@Override
	public Optional<Product> findById(UUID id) {
		return productJpaRepository.findById(id);
	}

	@Override
	public void deleteById(UUID productId) {
		productJpaRepository.deleteById(productId);
	}

	@Override
	public Page<Product> search(PageRequest pageRequest) {
		return null;
	}

	@Override
	public Page<Product> findAll(Pageable pageRequest) {
		return productJpaRepository.findAll(pageRequest);
	}

	@Override
	public Page<Product> findByTitleContaining(String keyword, Pageable pageable) {
		return productJpaRepository.findByTitleContaining(keyword, pageable);
	}

}
