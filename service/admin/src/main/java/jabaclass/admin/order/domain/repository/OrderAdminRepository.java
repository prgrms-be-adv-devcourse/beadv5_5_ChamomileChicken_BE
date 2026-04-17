package jabaclass.admin.order.domain.repository;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import jabaclass.admin.order.domain.model.Order;

public interface OrderAdminRepository {
	Page<Order> findAll(Pageable pageable);
}
