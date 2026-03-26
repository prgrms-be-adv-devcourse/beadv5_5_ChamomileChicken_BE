package jabaclass.product.presentation;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jabaclass.product.application.usecase.ProductUseCase;
import jabaclass.product.application.usecase.ScheduleUseCase;
import jabaclass.product.common.exception.ApiResponseDto;
import jabaclass.product.presentation.dto.request.CreateProductRequestDto;
import jabaclass.product.presentation.dto.request.CreateScheduleRequestDto;
import jabaclass.product.presentation.dto.request.OrderRequestDto;
import jabaclass.product.presentation.dto.request.SearchProductRequestDto;
import jabaclass.product.presentation.dto.request.UpdateProductRequestDto;
import jabaclass.product.presentation.dto.request.UpdateScheduleRequestDto;
import jabaclass.product.presentation.dto.respose.DeleteProductResposeDto;
import jabaclass.product.presentation.dto.respose.OrderResponseDto;
import jabaclass.product.presentation.dto.respose.ProductResponseDto;
import jabaclass.product.presentation.dto.respose.SchedulesResponseDto;
import jabaclass.product.presentation.dto.respose.SearchProductResponseDto;
import jabaclass.product.presentation.openapi.ProductOpenApi;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
@Slf4j
public class ProductRestController implements ProductOpenApi {

	private final ProductUseCase productUseCase;
	private final ScheduleUseCase scheduleUseCase;

	// 상품 등록
	@Override
	@PostMapping
	public ResponseEntity<ApiResponseDto<ProductResponseDto>> create(
		@RequestBody @Valid CreateProductRequestDto request
	) {
		ProductResponseDto response = productUseCase.create(request);

		return ResponseEntity.status(HttpStatus.CREATED)
			.body(ApiResponseDto.success(HttpStatus.CREATED, "성공적으로 등록 되었습니다.", response));
	}

	// 상품 수정
	@Override
	@PutMapping("/{productId}")
	public ResponseEntity<ApiResponseDto<ProductResponseDto>> change(
		@RequestBody @Valid UpdateProductRequestDto request,
		@PathVariable UUID productId
	) {
		ProductResponseDto response = productUseCase.update(request, productId);

		return ResponseEntity.ok()
			.body(ApiResponseDto.success(HttpStatus.OK, "성공적으로 수정 되었습니다.", response));
	}

	// 상품 삭제
	@Override
	@DeleteMapping("/{productId}")
	public ResponseEntity<ApiResponseDto<DeleteProductResposeDto>> delete(@PathVariable UUID productId) {
		DeleteProductResposeDto response = productUseCase.delete(productId);

		return ResponseEntity.ok()
			.body(ApiResponseDto.success(HttpStatus.OK, "성공적으로 삭제 되었습니다.", response));
	}

	// 상품 전체 검색
	@Override
	@GetMapping
	public ResponseEntity<ApiResponseDto<SearchProductResponseDto>> searchAllProduct(
		@ModelAttribute SearchProductRequestDto request) {
		SearchProductResponseDto response = productUseCase.searchAll(request);

		return ResponseEntity.ok()
			.body(ApiResponseDto.success(HttpStatus.OK, "성공적으로 전체 검색이 되었습니다.", response));
	}

	// 특정 상품 검색
	@Override
	@GetMapping("/{productId}")
	public ResponseEntity<ApiResponseDto<ProductResponseDto>> searchProduct(@PathVariable UUID productId) {
		ProductResponseDto response = productUseCase.searchById(productId);

		return ResponseEntity.ok()
			.body(ApiResponseDto.success(HttpStatus.OK, "성공적으로 검색이 되었습니다.", response));
	}

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
	@PostMapping("/reservations/release")
	public void schedulesVerification(@RequestBody OrderRequestDto requestDto) {
		scheduleUseCase.restoringInventory(requestDto);
	}

}
