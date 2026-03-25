package jabaclass.product.application.usecase;

import java.util.UUID;

import jabaclass.product.presentation.dto.request.CreateProductRequestDto;
import jabaclass.product.presentation.dto.request.SearchProductRequestDto;
import jabaclass.product.presentation.dto.request.UpdateProductRequestDto;
import jabaclass.product.presentation.dto.respose.DeleteProductResposeDto;
import jabaclass.product.presentation.dto.respose.ProductResponseDto;
import jabaclass.product.presentation.dto.respose.SearchProductReposeDto;

public interface ProductUseCase {

	// 상품 생성
	ProductResponseDto create(CreateProductRequestDto requestDto);

	// 상품 수정
	ProductResponseDto update(UpdateProductRequestDto requestDto, UUID productId);

	// 상품 삭제
	DeleteProductResposeDto delete(UUID productId);

	// 상품 전체 검색
	SearchProductReposeDto searchAll(SearchProductRequestDto requestDto);

	// 특정 상품 검색
	ProductResponseDto searchById(UUID productId);
}
