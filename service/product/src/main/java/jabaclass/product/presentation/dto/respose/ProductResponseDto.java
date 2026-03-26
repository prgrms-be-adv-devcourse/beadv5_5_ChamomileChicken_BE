package jabaclass.product.presentation.dto.respose;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;
import jabaclass.product.domain.model.Product;

@Schema(description = "상품 응답")
public record ProductResponseDto(
	@Schema(description = "상품 ID", example = "550e8400-e29b-41d4-a716-446655440000")
	UUID id,

	@Schema(description = "판매자 이름", example = "신짱구")
	String sellerName,

	@Schema(description = "상품명", example = "향수 공방")
	String title,

	@Schema(description = "총 인원수", example = "10")
	int maxCapacity,

	@Schema(description = "설명", example = "상품에 대한 설명입니다.")
	String description,

	@Schema(description = "설명이미지 ID", example = "550e8400-e29b-41d4-a716-446655440000")
	String descriptionImage,

	@Schema(description = "가격", example = "550000.00")
	BigDecimal price,

	@Schema(description = "상태", example = "공개")
	String statusName,

	@Schema(description = "등록자 ID", example = "22222222-2222-2222-2222-222222222222")
	UUID regId,

	@Schema(description = "등록일시", example = "2026-03-04T18:10:00")
	LocalDateTime regDt,

	@Schema(description = "수정자 ID", example = "33333333-3333-3333-3333-333333333333")
	UUID modifyId,

	@Schema(description = "수정일시", example = "2026-03-04T18:12:00")
	LocalDateTime modifyDt
) {

	public static ProductResponseDto from(Product product, String sellerName) {
		return new ProductResponseDto(
			product.getId(),
			sellerName,
			product.getTitle(),
			product.getMaxCapacity(),
			product.getDescription(),
			product.getDescriptionImage(),
			product.getPrice(),
			product.getStatus().getStatusName(),
			product.getRegId(),
			product.getRegDt(),
			product.getModifyId(),
			product.getModifyDt()
		);
	}

	public static ProductResponseDto listFrom(Product product, Map<UUID, String> map) {
		return new ProductResponseDto(
			product.getId(),
			map.getOrDefault(product.getSellerId(), "사용자 이름이 지정되지 않았습니다."),
			product.getTitle(),
			product.getMaxCapacity(),
			product.getDescription(),
			product.getDescriptionImage(),
			product.getPrice(),
			product.getStatus().getStatusName(),
			product.getRegId(),
			product.getRegDt(),
			product.getModifyId(),
			product.getModifyDt()
		);
	}
}
