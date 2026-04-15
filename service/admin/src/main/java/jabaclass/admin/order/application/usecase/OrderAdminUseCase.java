package jabaclass.admin.order.application.usecase;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import jabaclass.admin.order.presentation.dto.response.OrderAdminResponseDto;

public interface OrderAdminUseCase {

	Page<OrderAdminResponseDto> getOrders(Pageable pageable);

}
