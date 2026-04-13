package jabaclass.product.presentation.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jabaclass.product.application.usecase.ScheduleUseCase;
import jabaclass.product.common.exception.ApiResponseDto;
import jabaclass.product.presentation.dto.request.CreateScheduleRequestDto;
import jabaclass.product.presentation.dto.request.OrderRequestDto;
import jabaclass.product.presentation.dto.request.UpdateScheduleRequestDto;
import jabaclass.product.presentation.dto.respose.AvailabilityScheduleResponseDto;
import jabaclass.product.presentation.dto.respose.DeleteScheduleResposeDto;
import jabaclass.product.presentation.dto.respose.OrderResponseDto;
import jabaclass.product.presentation.dto.respose.SchedulesResponseDto;
import jabaclass.product.presentation.openapi.SchedulesOpenApi;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
@Slf4j
public class SchdulesRestController implements SchedulesOpenApi {

	private final ScheduleUseCase scheduleUseCase;

	// 상품 일정 등록
	@Override
	@PostMapping("/{productId}/schedules")
	public ResponseEntity<ApiResponseDto<SchedulesResponseDto>> schedulesCreate(
		@RequestBody @Valid CreateScheduleRequestDto requestDto
		, @PathVariable UUID productId) {

		SchedulesResponseDto response = scheduleUseCase.create(requestDto, productId);

		return ResponseEntity.status(HttpStatus.CREATED)
			.body(ApiResponseDto.success(HttpStatus.CREATED, "성공적으로 등록 되었습니다.", response));
	}

	// 상품 일정 수정
	@Override
	@PutMapping("/{productId}/schedules/{scheduleId}")
	public ResponseEntity<ApiResponseDto<SchedulesResponseDto>> schedulesUpdate(
		@RequestBody @Valid UpdateScheduleRequestDto requestDto,
		@PathVariable UUID productId,
		@PathVariable UUID scheduleId
	) {
		SchedulesResponseDto response = scheduleUseCase.update(requestDto, productId, scheduleId);

		return ResponseEntity.ok()
			.body(ApiResponseDto.success(HttpStatus.OK, "성공적으로 수정 되었습니다.", response));
	}

	// 상품 스케줄 검증 -> 예약 가능한지
	@Override
	@PostMapping("/reservations")
	public ResponseEntity<OrderResponseDto> schedulesReservations(@RequestBody OrderRequestDto requestDto) {
		OrderResponseDto response = scheduleUseCase.verification(requestDto);
		return ResponseEntity.ok().body(response);
	}

	@Override
	@DeleteMapping("/{productId}/schedules/{scheduleId}")
	public ResponseEntity<ApiResponseDto<DeleteScheduleResposeDto>> schedulesDelete(@PathVariable UUID productId,
		@PathVariable UUID scheduleId) {
		DeleteScheduleResposeDto response = scheduleUseCase.delete(productId, scheduleId);

		return ResponseEntity.ok()
			.body(ApiResponseDto.success(HttpStatus.OK, "성공적으로 삭제 되었습니다.", response));
	}

	@Override
	@GetMapping("/{productId}/schedules")
	public ResponseEntity<ApiResponseDto<List<SchedulesResponseDto>>> schedulesSelectList(
		@PathVariable UUID productId) {
		List<SchedulesResponseDto> response = scheduleUseCase.schedulesList(productId);

		return ResponseEntity.ok()
			.body(ApiResponseDto.success(HttpStatus.OK, "성공적으로 검색 되었습니다.", response));
	}

	@Override
	@GetMapping("/{scheduleId}/availability")
	public ResponseEntity<ApiResponseDto<AvailabilityScheduleResponseDto>> schedulesaAvailability(
		@PathVariable UUID scheduleId) {
		AvailabilityScheduleResponseDto response = scheduleUseCase.availabilitySchedule(scheduleId);

		return ResponseEntity.ok()
			.body(ApiResponseDto.success(HttpStatus.OK, "성공적으로 검색 되었습니다.", response));
	}
}
