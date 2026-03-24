package jabaclass.product.application.usecase;

import jabaclass.product.presentation.dto.request.CreateProductRequestDto;
import jabaclass.product.presentation.dto.respose.CreateProductResponseDto;

public interface ProductUseCase {

	CreateProductResponseDto create(CreateProductRequestDto requestDto);
}
