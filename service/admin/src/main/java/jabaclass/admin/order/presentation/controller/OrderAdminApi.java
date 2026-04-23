package jabaclass.admin.order.presentation.controller;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestParam;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jabaclass.admin.common.dto.ApiResponseDto;
import jabaclass.admin.order.presentation.dto.response.OrderAdminResponseDto;

@Tag(name = "Admin - Order", description = "어드민 주문 조회 API")
public interface OrderAdminApi {

	@Operation(summary = "전체 주문 조회 (필터링/검색)")
	ResponseEntity<ApiResponseDto<Page<OrderAdminResponseDto>>> getOrders(
		Pageable pageable,
		@Parameter(description = "상태 필터 (PENDING/PAID/FAILED/REFUNDED/EXPIRED)") @RequestParam(required = false) String status,
		@Parameter(description = "판매자 ID 필터") @RequestParam(required = false) UUID sellerId,
		@Parameter(description = "시작 날짜 (createdAt 기준)") @RequestParam(required = false) LocalDateTime startDate,
		@Parameter(description = "종료 날짜 (createdAt 기준)") @RequestParam(required = false) LocalDateTime endDate
	);
}
