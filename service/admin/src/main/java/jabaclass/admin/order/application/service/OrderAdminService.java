package jabaclass.admin.order.application.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jabaclass.admin.order.application.usecase.OrderAdminUseCase;
import jabaclass.admin.order.domain.repository.OrderAdminRepository;
import jabaclass.admin.order.presentation.dto.response.OrderAdminResponseDto;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OrderAdminService implements OrderAdminUseCase {

	private final OrderAdminRepository orderAdminRepository;

	@Override
	@Transactional(readOnly = true, transactionManager = "orderTransactionManager")
	public Page<OrderAdminResponseDto> getOrders(Pageable pageable) {
		return orderAdminRepository.findAll(pageable)
			.map(OrderAdminResponseDto::from);
	}
}
