package jabaclass.product.presentation.openapi;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jabaclass.product.common.exception.ApiResponseDto;
import jabaclass.product.domain.model.status.OrderStatus;
import jabaclass.product.presentation.dto.request.CreateProductRequestDto;
import jabaclass.product.presentation.dto.request.CreateScheduleRequestDto;
import jabaclass.product.presentation.dto.request.OrderRequestDto;
import jabaclass.product.presentation.dto.request.SearchProductRequestDto;
import jabaclass.product.presentation.dto.request.UpdateProductRequestDto;
import jabaclass.product.presentation.dto.request.UpdateScheduleRequestDto;
import jabaclass.product.presentation.dto.respose.DeleteProductResposeDto;
import jabaclass.product.presentation.dto.respose.DeleteScheduleResposeDto;
import jabaclass.product.presentation.dto.respose.OrderResponseDto;
import jabaclass.product.presentation.dto.respose.ProductResponseDto;
import jabaclass.product.presentation.dto.respose.ProductUserResponseDto;
import jabaclass.product.presentation.dto.respose.SchedulesResponseDto;
import jabaclass.product.presentation.dto.respose.SearchProductResponseDto;

@Tag(name = "Product", description = "상품 API")
public interface ProductOpenApi {

	@Operation(summary = "상품 생성", description = "신규 상품을 생성 합니다.")
	@ApiResponse(
		responseCode = "201",
		description = "상품 등록 성공",
		content = @Content(
			schema = @Schema(implementation = ApiResponseDto.class)
		)
	)
	@CommonErrorResponses
	ResponseEntity<ApiResponseDto<ProductResponseDto>> create(CreateProductRequestDto request);

	@Operation(summary = "상품 수정", description = "상품을 수정 합니다.")
	@ApiResponse(
		responseCode = "200",
		description = "상품 수정 성공",
		content = @Content(
			schema = @Schema(implementation = ApiResponseDto.class)
		)
	)
	@CommonErrorResponses
	ResponseEntity<ApiResponseDto<ProductResponseDto>> change(UpdateProductRequestDto request, UUID productId);

	@Operation(summary = "상품 삭제", description = "상품을 삭제 합니다.")
	@ApiResponse(
		responseCode = "202",
		description = "상품 삭제 성공",
		content = @Content(
			schema = @Schema(implementation = ApiResponseDto.class)
		)
	)
	@CommonErrorResponses
	ResponseEntity<ApiResponseDto<DeleteProductResposeDto>> delete(UUID productId);

	@Operation(summary = "상품 전체 검색", description = "전체 상품을 검색 합니다.")
	@ApiResponse(
		responseCode = "202",
		description = "상품 검색 성공",
		content = @Content(
			schema = @Schema(implementation = ApiResponseDto.class)
		)
	)
	@CommonErrorResponses
	ResponseEntity<ApiResponseDto<SearchProductResponseDto>> searchAllProduct(SearchProductRequestDto request);

	@Operation(summary = "특정 상품 검색", description = "특정 상품을 검색 합니다.")
	@ApiResponse(
		responseCode = "202",
		description = "상품 검색 성공",
		content = @Content(
			schema = @Schema(implementation = ApiResponseDto.class)
		)
	)
	@CommonErrorResponses
	ResponseEntity<ApiResponseDto<ProductResponseDto>> searchProduct(UUID productId);

	@Operation(summary = "상품 일정 등록", description = "상품 일정을 등록 합니다.")
	@ApiResponse(
		responseCode = "201",
		description = "상품 일정 등록 성공",
		content = @Content(
			schema = @Schema(implementation = ApiResponseDto.class)
		)
	)
	@CommonErrorResponses
	ResponseEntity<ApiResponseDto<SchedulesResponseDto>> schedulesCreate(CreateScheduleRequestDto requestDto,
		UUID productId);

	@Operation(summary = "상품 일정 수정", description = "상품 일정을 수정 합니다.")
	@ApiResponse(
		responseCode = "200",
		description = "상품 일정 수정 성공",
		content = @Content(
			schema = @Schema(implementation = ApiResponseDto.class)
		)
	)
	@CommonErrorResponses
	ResponseEntity<ApiResponseDto<SchedulesResponseDto>> schedulesUpdate(UpdateScheduleRequestDto requestDto,
		UUID productId, UUID scheduleId);

	@Operation(summary = "상품 일정 검증", description = "상품 일정을 검증 합니다.")
	@ApiResponse(
		responseCode = "200",
		description = "스케줄 검증 성공",
		content = @Content(
			schema = @Schema(implementation = OrderResponseDto.class)
		)
	)
	@CommonErrorResponses
	ResponseEntity<OrderResponseDto> schedulesReservations(OrderRequestDto requestDto);

	@Operation(summary = "상품 일정 재고 상태 변경", description = "상품 일정의 재고 상태를 변경 합니다.")
	@ApiResponse(
		responseCode = "200",
		description = "재고 추가 성공"
	)
	@CommonErrorResponses
	void schedulesVerification(OrderRequestDto requestDto);

	@Operation(summary = "상품 일정 삭제", description = "상품 일정을 삭제 합니다.")
	@ApiResponse(
		responseCode = "200",
		description = "스케줄 삭제 성공",
		content = @Content(
			schema = @Schema(implementation = DeleteScheduleResposeDto.class)
		)
	)
	@CommonErrorResponses
	ResponseEntity<ApiResponseDto<DeleteScheduleResposeDto>> schedulesDelete(UUID productId, UUID scheduleId);

	@Operation(summary = "상품 일정 검색", description = "상품 일정을 검색 합니다.")
	@ApiResponse(
		responseCode = "200",
		description = "스케줄 검색 성공",
		content = @Content(
			array = @io.swagger.v3.oas.annotations.media.ArraySchema(
				schema = @Schema(implementation = SchedulesResponseDto.class))
		)
	)
	@CommonErrorResponses
	ResponseEntity<ApiResponseDto<List<SchedulesResponseDto>>> schedulesSelectList(UUID productId);

	@Operation(summary = "예약 유저 조회", description = "예약 유저 조회를 합니다.")
	@ApiResponse(
		responseCode = "200",
		description = "예약 유저 조회 성공",
		content = @Content(
			array = @io.swagger.v3.oas.annotations.media.ArraySchema(
				schema = @Schema(implementation = ProductUserResponseDto.class))
		)
	)
	@CommonErrorResponses
	ResponseEntity<ApiResponseDto<List<ProductUserResponseDto>>> schedulesSelectUser(UUID scheduleId);

	@Operation(summary = "예약 유저 삭제", description = "예약 유저 삭제 입니다.")
	@ApiResponse(
		responseCode = "200",
		description = "예약 삭제 성공",
		content = @Content(
			schema = @Schema(implementation = ProductUserResponseDto.class)
		)
	)
	@CommonErrorResponses
	ResponseEntity<ApiResponseDto<ProductUserResponseDto>> schedulesDeleteUser(UUID productUserId);

	@Operation(summary = "예약 유저 수정", description = "예약 유저 수정 입니다.")
	@ApiResponse(
		responseCode = "200",
		description = "예약 유저 성공",
		content = @Content(
			schema = @Schema(implementation = ProductUserResponseDto.class)
		)
	)
	@CommonErrorResponses
	ResponseEntity<ApiResponseDto<ProductUserResponseDto>> schedulesUpdateUserStatus(OrderStatus status);

}
