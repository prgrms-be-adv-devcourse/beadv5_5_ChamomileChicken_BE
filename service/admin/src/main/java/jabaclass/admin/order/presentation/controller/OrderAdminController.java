package jabaclass.admin.order.presentation.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jabaclass.admin.common.dto.ApiResponseDto;
import jabaclass.admin.order.application.usecase.OrderAdminUseCase;
import jabaclass.admin.order.presentation.dto.response.OrderAdminResponseDto;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/admins/orders")
@RequiredArgsConstructor
public class OrderAdminController implements OrderAdminApi {

	private final OrderAdminUseCase orderAdminUseCase;

	@Override
	@GetMapping
	public ResponseEntity<ApiResponseDto<Page<OrderAdminResponseDto>>> getOrders(Pageable pageable) {
		return ResponseEntity.ok(
			ApiResponseDto.success(HttpStatus.OK, "주문 목록 조회 성공", orderAdminUseCase.getOrders(pageable))
		);
	}
}
