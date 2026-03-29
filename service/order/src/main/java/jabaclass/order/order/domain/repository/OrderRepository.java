package jabaclass.order.order.domain.repository;

import jabaclass.order.order.domain.model.Order;
import jabaclass.order.order.domain.model.OrderStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrderRepository {

    Order save(Order order);

    Optional<Order> findById(UUID orderId);

    List<Order> findAllByUserId(UUID userId);

    List<Order> findAllByUserIdAndStatus(UUID userId, OrderStatus status);
}
