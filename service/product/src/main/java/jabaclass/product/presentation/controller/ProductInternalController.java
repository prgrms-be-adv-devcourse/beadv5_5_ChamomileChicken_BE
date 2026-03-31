package jabaclass.product.presentation.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jabaclass.product.application.usecase.ProductUseCase;
import jabaclass.product.presentation.dto.request.ProductBulkReadRequestDto;
import jabaclass.product.presentation.dto.respose.ProductSettlementItemResponseDto;
import jabaclass.product.presentation.openapi.ProductInternalOpenApi;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/products")
public class ProductInternalController implements ProductInternalOpenApi {

	private final ProductUseCase productUseCase;

	@Override
	@PostMapping("/bulk")
	public ResponseEntity<List<ProductSettlementItemResponseDto>> getProductsByIds(
		@Valid @RequestBody ProductBulkReadRequestDto requestDto
	) {
		return ResponseEntity.ok(productUseCase.getProductsByIds(requestDto.productIds()));
	}
}
