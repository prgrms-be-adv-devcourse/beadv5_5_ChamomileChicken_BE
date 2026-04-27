package jabaclass.admin.order.infrastructure.persistence;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import jabaclass.admin.order.domain.dto.OrderSearchCondition;
import jabaclass.admin.order.domain.model.Order;
import jabaclass.admin.order.domain.model.OrderStatus;
import jabaclass.admin.order.domain.repository.OrderAdminRepository;
import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class OrderAdminRepositoryAdapter implements OrderAdminRepository {

	private final OrderAdminJpaRepository orderAdminJpaRepository;

	@Override
	public Page<Order> findAll(OrderSearchCondition condition, Pageable pageable) {
		return orderAdminJpaRepository.findAll(OrderAdminSpecification.withCondition(condition), pageable);
	}

	@Override
	public long countByStatus(OrderStatus status) {
		return orderAdminJpaRepository.countByStatus(status);
	}

	@Override
	public List<Object[]> findMonthlyOrderStats(int year) {
		return orderAdminJpaRepository.findMonthlyOrderStats(year);
	}
}
