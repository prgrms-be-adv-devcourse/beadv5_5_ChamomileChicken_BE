package jabaclass.admin.product.presentation.controller;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jabaclass.admin.common.dto.ApiResponseDto;
import jabaclass.admin.product.application.usecase.ProductAdminUseCase;
import jabaclass.admin.product.presentation.dto.response.ProductAdminResponseDto;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/admins/products")
@RequiredArgsConstructor
public class ProductAdminController implements ProductAdminApi {

	private final ProductAdminUseCase productAdminUseCase;

	@Override
	@GetMapping
	public ResponseEntity<ApiResponseDto<Page<ProductAdminResponseDto>>> getProducts(Pageable pageable) {
		return ResponseEntity.ok(
			ApiResponseDto.success(HttpStatus.OK, "상품 목록 조회 성공", productAdminUseCase.getProducts(pageable))
		);
	}

	@Override
	@PatchMapping("/{productId}/force-down")
	public ResponseEntity<ApiResponseDto<Void>> forceDownProduct(@PathVariable UUID productId) {
		productAdminUseCase.forceDownProduct(productId);
		return ResponseEntity.ok(
			ApiResponseDto.success(HttpStatus.OK, "상품 강제 내리기 완료", null)
		);
	}
}
