package jabaclass.admin.order.infrastructure.persistence;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import jabaclass.admin.order.domain.model.Order;

public interface OrderAdminJpaRepository extends JpaRepository<Order, UUID> {
}
