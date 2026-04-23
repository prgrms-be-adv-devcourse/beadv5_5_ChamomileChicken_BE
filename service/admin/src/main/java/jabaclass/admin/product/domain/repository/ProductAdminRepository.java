package jabaclass.admin.product.domain.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import jabaclass.admin.product.domain.dto.ProductSearchCondition;
import jabaclass.admin.product.domain.model.Product;

public interface ProductAdminRepository {
	Page<Product> findAll(Pageable pageable);
	Page<Product> findAll(ProductSearchCondition condition, Pageable pageable);
	Optional<Product> findById(UUID productId);
}
