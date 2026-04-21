package jabaclass.product.application.usecase;

import java.util.List;
import java.util.UUID;

import jabaclass.product.domain.model.Product;
import jabaclass.product.presentation.dto.request.CreateProductRequestDto;
import jabaclass.product.presentation.dto.request.SearchProductRequestDto;
import jabaclass.product.presentation.dto.request.UpdateProductRequestDto;
import jabaclass.product.presentation.dto.respose.DeleteProductResposeDto;
import jabaclass.product.presentation.dto.respose.ProductResponseDto;
import jabaclass.product.presentation.dto.respose.ProductSettlementItemResponseDto;
import jabaclass.product.presentation.dto.respose.SearchProductResponseDto;

public interface ProductUseCase {

	// 상품 생성
	ProductResponseDto create(CreateProductRequestDto requestDto, UUID sellerId);

	// 상품 수정
	ProductResponseDto update(UpdateProductRequestDto requestDto, UUID productId, UUID sellerId);

	// 상품 삭제
	DeleteProductResposeDto delete(UUID productId, UUID sellerId);

	// 상품 전체 검색
	SearchProductResponseDto searchAll(SearchProductRequestDto requestDto);

	// 특정 상품 검색
	ProductResponseDto searchById(UUID productId, UUID userId);

	// 상품 존재 여부/단일 상품 검색
	Product findByIdOrThrow(UUID productId);

	// 해당 상품 보유자인지 확인
	Product matchProductAndSellerId(UUID productId, UUID sellerId);

	List<ProductSettlementItemResponseDto> getProductsByIds(List<UUID> productIds);

	// PostgreSQL → ES 초기 마이그레이션
	// 새 상품은 생성/수정 시점에 자동으로 ES에 색인되지만, ES 도입 이전에 이미 RDB에 쌓인 상품들은 ES에 없는 상태라 검색이 안 됨. 이걸 한 번에 밀어넣기 위한 엔드포인트
	int migrateToEs();
}
