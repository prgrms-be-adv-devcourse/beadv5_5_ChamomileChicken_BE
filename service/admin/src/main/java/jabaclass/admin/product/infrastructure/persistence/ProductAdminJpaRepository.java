package jabaclass.admin.product.infrastructure.persistence;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import jabaclass.admin.product.domain.model.Product;
import jabaclass.admin.product.domain.model.ProductStatus;

public interface ProductAdminJpaRepository extends JpaRepository<Product, UUID>, JpaSpecificationExecutor<Product> {

	long countByStatusAndDeleteDtIsNull(ProductStatus status);
}
