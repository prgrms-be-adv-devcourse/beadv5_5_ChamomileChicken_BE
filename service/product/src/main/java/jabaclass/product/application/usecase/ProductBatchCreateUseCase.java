package jabaclass.product.application.usecase;

import java.util.UUID;

import jabaclass.product.presentation.dto.request.CreateProductBatchRequestDto;
import jabaclass.product.presentation.dto.response.ProductBatchCreateResponseDto;

public interface ProductBatchCreateUseCase {

	ProductBatchCreateResponseDto createBatch(CreateProductBatchRequestDto requestDto, UUID sellerId);
}
