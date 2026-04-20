package jabaclass.admin.product.infrastructure.persistence;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import jabaclass.admin.product.domain.model.Product;

public interface ProductAdminJpaRepository extends JpaRepository<Product, UUID> {
}
