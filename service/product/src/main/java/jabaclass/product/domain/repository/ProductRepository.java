package jabaclass.product.domain.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import jabaclass.product.domain.model.Product;
import jabaclass.product.domain.model.ProductStatus;

public interface ProductRepository {
	Product save(Product product);

	Optional<Product> findById(UUID id);

	Page<Product> findAll(Pageable pageRequest);

	Page<Product> findByStatusAndTitleContainingAndDeleteDtIsNull(ProductStatus status, String keyword,
		Pageable pageable);

	Page<Product> findByStatusAndDeleteDtIsNull(ProductStatus status, Pageable pageable);
}
