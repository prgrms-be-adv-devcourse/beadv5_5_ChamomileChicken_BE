package jabaclass.admin.order.infrastructure.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import jabaclass.admin.order.domain.model.Order;
import jabaclass.admin.order.domain.repository.OrderAdminRepository;
import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class OrderAdminRepositoryAdapter implements OrderAdminRepository {

	private final OrderAdminJpaRepository orderAdminJpaRepository;

	@Override
	public Page<Order> findAll(Pageable pageable) {
		return orderAdminJpaRepository.findAll(pageable);
	}
}
