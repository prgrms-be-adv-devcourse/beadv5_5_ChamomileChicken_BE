package jabaclass.admin.order.infrastructure.persistence;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jabaclass.admin.order.domain.model.Order;
import jabaclass.admin.order.domain.model.OrderStatus;

public interface OrderAdminJpaRepository extends JpaRepository<Order, UUID>, JpaSpecificationExecutor<Order> {

	long countByStatus(OrderStatus status);

	@Query("SELECT MONTH(o.createdAt), " +
		"SUM(CASE WHEN o.status = jabaclass.admin.order.domain.model.OrderStatus.PAID THEN 1 ELSE 0 END), " +
		"SUM(CASE WHEN o.status = jabaclass.admin.order.domain.model.OrderStatus.PAID THEN o.price ELSE 0 END) " +
		"FROM Order o WHERE YEAR(o.createdAt) = :year " +
		"GROUP BY MONTH(o.createdAt) ORDER BY MONTH(o.createdAt)")
	List<Object[]> findMonthlyOrderStats(@Param("year") int year);
}
