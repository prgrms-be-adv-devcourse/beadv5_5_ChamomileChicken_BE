package jabaclass.product.infrastructure.persistence;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import jabaclass.product.domain.model.Products;
import jabaclass.product.domain.model.status.ProductStatus;

public interface ProductJpaRepository extends JpaRepository<Products, UUID> {
	Page<Products> findByStatusAndTitleContainingAndDeleteDtIsNull(ProductStatus status, String keyword,
		Pageable pageable);

	Page<Products> findByStatusAndDeleteDtIsNull(ProductStatus status, Pageable pageable);

	Optional<Products> findByIdAndSellerId(UUID productId, UUID sellerId);
}
