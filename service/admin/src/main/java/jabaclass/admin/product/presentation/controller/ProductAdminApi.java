package jabaclass.admin.product.presentation.controller;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jabaclass.admin.common.dto.ApiResponseDto;
import jabaclass.admin.product.presentation.dto.response.ProductAdminResponseDto;

@Tag(name = "Admin - Product", description = "어드민 상품 관리 API")
public interface ProductAdminApi {

	@Operation(summary = "전체 상품 조회")
	ResponseEntity<ApiResponseDto<Page<ProductAdminResponseDto>>> getProducts(Pageable pageable);

	@Operation(summary = "상품 강제 내리기")
	ResponseEntity<ApiResponseDto<Void>> forceDownProduct(@PathVariable UUID productId);
}
