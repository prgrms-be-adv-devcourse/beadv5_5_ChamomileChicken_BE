package jabaclass.order.order.infrastructure.persistence;

import jabaclass.order.order.domain.model.Order;
import jabaclass.order.order.domain.model.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface OrderJpaRepository extends JpaRepository<Order, UUID> {

    List<Order> findAllByUserId(UUID userId);

    List<Order> findAllByUserIdAndStatus(UUID userId, OrderStatus status);
}
