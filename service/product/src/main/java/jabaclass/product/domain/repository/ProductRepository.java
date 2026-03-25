package jabaclass.product.domain.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import jabaclass.product.domain.model.Product;

public interface ProductRepository {
	Product save(Product product);

	Optional<Product> findById(UUID id);

	void deleteById(UUID productId);

	Page<Product> findAll(Pageable pageRequest);

	Page<Product> findByTitleContaining(String keyword, Pageable pageable);
}
