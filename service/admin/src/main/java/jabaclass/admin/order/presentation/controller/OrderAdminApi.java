package jabaclass.admin.order.presentation.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jabaclass.admin.common.dto.ApiResponseDto;
import jabaclass.admin.order.presentation.dto.response.OrderAdminResponseDto;

@Tag(name = "Admin - Order", description = "어드민 주문 조회 API")
public interface OrderAdminApi {

	@Operation(summary = "전체 주문 조회")
	ResponseEntity<ApiResponseDto<Page<OrderAdminResponseDto>>> getOrders(Pageable pageable);
}
