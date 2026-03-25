package jabaclass.product.infrastructure.persistence;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import jabaclass.product.domain.model.Product;
import jabaclass.product.domain.model.ProductStatus;

public interface ProductJpaRepository extends JpaRepository<Product, UUID> {
	Page<Product> findByStatusAndTitleContainingAndDeleteDtIsNull(ProductStatus status, String keyword,
		Pageable pageable);

	Page<Product> findByStatusAndDeleteDtIsNull(ProductStatus status, Pageable pageable);

	Optional<Product> findByIdAndSellerId(UUID productId, UUID sellerId);
}
