package jabaclass.product.presentation;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jabaclass.product.application.usecase.ProductUseCase;
import jabaclass.product.common.exception.ApiResponseDto;
import jabaclass.product.presentation.dto.request.CreateProductRequestDto;
import jabaclass.product.presentation.dto.respose.CreateProductResponseDto;
import jabaclass.product.presentation.openapi.ProductOpenApi;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/product")
@RequiredArgsConstructor
@Slf4j
public class ProductRestController implements ProductOpenApi {

	private final ProductUseCase useCase;

	@Override
	@PostMapping
	public ResponseEntity<ApiResponseDto<CreateProductResponseDto>> create(
		@RequestBody @Valid CreateProductRequestDto request) {
		CreateProductResponseDto respose = useCase.create(request);
		/*return ResponseEntity.ok(

		);*/
		return ResponseEntity.status(HttpStatus.CREATED)
			.body(ApiResponseDto.success(HttpStatus.CREATED, "성공적으로 등록되었습니다.", respose));
	}

}
