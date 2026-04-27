package jabaclass.admin.order.domain.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import jabaclass.admin.order.domain.dto.OrderSearchCondition;
import jabaclass.admin.order.domain.model.Order;
import jabaclass.admin.order.domain.model.OrderStatus;

public interface OrderAdminRepository {
	Page<Order> findAll(OrderSearchCondition condition, Pageable pageable);
	long countByStatus(OrderStatus status);
	List<Object[]> findMonthlyOrderStats(int year);
}
