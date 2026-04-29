package jabaclass.product.application.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jabaclass.product.application.usecase.ProductBatchCreateUseCase;
import jabaclass.product.application.usecase.ProductUseCase;
import jabaclass.product.presentation.dto.request.CreateProductBatchRequestDto;
import jabaclass.product.presentation.dto.request.CreateProductRequestDto;
import jabaclass.product.presentation.dto.response.ProductBatchCreateResponseDto;
import jabaclass.product.presentation.dto.response.ProductResponseDto;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProductBatchCreateService implements ProductBatchCreateUseCase {

	private final ProductUseCase productUseCase;

	@Override
	@Transactional
	public ProductBatchCreateResponseDto createBatch(CreateProductBatchRequestDto requestDto, UUID sellerId) {
		List<ProductResponseDto> createdProducts = requestDto.products().stream()
			.map(product -> overwriteSeller(product, sellerId))
			.map(product -> productUseCase.create(product, sellerId))
			.toList();

		return ProductBatchCreateResponseDto.of(createdProducts);
	}

	private CreateProductRequestDto overwriteSeller(
		CreateProductRequestDto requestDto,
		UUID sellerId
	) {
		return new CreateProductRequestDto(
			sellerId,
			requestDto.title(),
			requestDto.maxCapacity(),
			requestDto.description(),
			requestDto.imageIds(),
			requestDto.price(),
			requestDto.status(),
			requestDto.roadAddress(),
			requestDto.detailAddress(),
			requestDto.zonecode(),
			requestDto.latitude(),
			requestDto.longitude()
		);
	}
}
