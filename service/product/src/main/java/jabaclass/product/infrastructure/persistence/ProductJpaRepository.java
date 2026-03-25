package jabaclass.product.infrastructure.persistence;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import jabaclass.product.domain.model.Product;

public interface ProductJpaRepository extends JpaRepository<Product, UUID> {
	Page<Product> findByTitleContaining(String keyword, Pageable pageable);
}
