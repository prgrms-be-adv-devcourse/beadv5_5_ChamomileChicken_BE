package jabaclass.product.presentation.controller;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jabaclass.product.application.usecase.ProductBatchCreateUseCase;
import jabaclass.product.common.auth.CurrentUser;
import jabaclass.product.common.exception.ApiResponseDto;
import jabaclass.product.presentation.dto.request.CreateProductBatchRequestDto;
import jabaclass.product.presentation.dto.response.ProductBatchCreateResponseDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/products/batch")
@RequiredArgsConstructor
public class ProductBatchCreateController {

	private final ProductBatchCreateUseCase productBatchCreateUseCase;

	@PostMapping
	public ResponseEntity<ApiResponseDto<ProductBatchCreateResponseDto>> createBatchProducts(
		@RequestBody @Valid CreateProductBatchRequestDto request,
		@CurrentUser UUID userId
	) {
		ProductBatchCreateResponseDto response = productBatchCreateUseCase.createBatch(request, userId);

		return ResponseEntity.status(HttpStatus.CREATED)
			.body(ApiResponseDto.success(HttpStatus.CREATED, "성공적으로 상품이 일괄 생성되었습니다.", response));
	}
}
