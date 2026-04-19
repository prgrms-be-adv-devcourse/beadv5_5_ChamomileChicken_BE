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
import jabaclass.product.presentation.dto.request.CreateScheduleRequestDto;
import jabaclass.product.presentation.dto.request.OrderRequestDto;
import jabaclass.product.presentation.dto.request.UpdateScheduleRequestDto;
import jabaclass.product.presentation.dto.response.AvailabilityScheduleResponseDto;
import jabaclass.product.presentation.dto.response.DeleteScheduleResposeDto;
import jabaclass.product.presentation.dto.response.OrderResponseDto;
import jabaclass.product.presentation.dto.response.SchedulesResponseDto;

@Tag(name = "Schedules", description = "상품 일정 API")
public interface SchedulesOpenApi {

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
		UUID productId, UUID userId);

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
		UUID productId, UUID scheduleId, UUID userId);

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

	@Operation(summary = "상품 일정 삭제", description = "상품 일정을 삭제 합니다.")
	@ApiResponse(
		responseCode = "200",
		description = "스케줄 삭제 성공",
		content = @Content(
			schema = @Schema(implementation = DeleteScheduleResposeDto.class)
		)
	)
	@CommonErrorResponses
	ResponseEntity<ApiResponseDto<DeleteScheduleResposeDto>> schedulesDelete(UUID productId, UUID scheduleId,
		UUID userId);

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

	@Operation(summary = "상품 일정 예약 상태 확인", description = "상품 일정의 예약 상태를 확인 합니다.")
	@ApiResponse(
		responseCode = "200",
		description = "스케줄 예약 상태 확인 성공",
		content = @Content(
			schema = @Schema(implementation = SchedulesResponseDto.class)
		)
	)
	@CommonErrorResponses
	ResponseEntity<ApiResponseDto<AvailabilityScheduleResponseDto>> schedulesaAvailability(UUID scheduleId);

	@Operation(summary = "상품 일정 상제 정보 확인", description = "상품 일정 상제 정보를 검색 합니다.")
	@ApiResponse(
		responseCode = "200",
		description = "상품 일정 상제 정보 검색 성공",
		content = @Content(
			schema = @Schema(implementation = SchedulesResponseDto.class)
		)
	)
	@CommonErrorResponses
	ResponseEntity<ApiResponseDto<SchedulesResponseDto>> selectSchedules(UUID productUserId);

}
