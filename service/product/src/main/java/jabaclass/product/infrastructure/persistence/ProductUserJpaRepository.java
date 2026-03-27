package jabaclass.product.infrastructure.persistence;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import jabaclass.product.domain.model.ProductUser;

public interface ProductUserJpaRepository extends JpaRepository<ProductUser, UUID> {
	List<ProductUser> findByProductScheduleId(UUID scheduleId);
}
