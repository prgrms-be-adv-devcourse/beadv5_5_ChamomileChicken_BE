package jabaclass.product.infrastructure.persistence;

import java.util.Optional;
import java.util.UUID;

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

}
