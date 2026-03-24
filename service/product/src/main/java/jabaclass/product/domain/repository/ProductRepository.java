package jabaclass.product.domain.repository;

import java.util.Optional;
import java.util.UUID;

import jabaclass.product.domain.model.Product;

public interface ProductRepository {
	Product save(Product product);

	Optional<Product> findById(UUID id);
}
