package jabaclass.product.presentation.dto.request;

import java.math.BigDecimal;

import io.swagger.v3.oas.annotations.media.Schema;
import jabaclass.product.domain.model.ProductStatus;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "상품 수정")
public record UpdateProductRequestDto(

	@NotBlank(message = "상품명을 입력해주세요.")
	@Size(max = 100)
	@Schema(description = "상품명", example = "향수 공방")
	String title,

	@Min(value = 1, message = "예약 가능 인원수를 입력해주세요.")
	@Schema(description = "총 인원수", example = "10")
	int maxCapacity,

	@Schema(description = "설명", example = "상품에 대한 설명입니다.")
	String description,

	@Schema(description = "설명이미지 ID", example = "550e8400-e29b-41d4-a716-446655440000")
	String descriptionImage,

	@Min(value = 1, message = "가격은 1원 이상이어야 합니다.")
	@Schema(description = "가격", example = "550000.00")
	BigDecimal price,

	@Schema(description = "상태", example = "ENABLE")
	ProductStatus status
) {

}
