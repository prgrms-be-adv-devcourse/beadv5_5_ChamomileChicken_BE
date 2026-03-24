package jabaclass.product.infrastructure.persistence;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import jabaclass.product.domain.model.Product;

public interface ProductJpaRepository extends JpaRepository<Product, UUID> {
}
