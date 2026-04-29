package jabaclass.product.presentation.dto.request;

import java.util.List;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

@Schema(description = "상품 일괄 생성 요청")
public record CreateProductBatchRequestDto(
	@NotEmpty(message = "생성할 상품 목록은 비어 있을 수 없습니다.")
	@Size(max = 20, message = "한 번에 최대 20개까지만 생성할 수 있습니다.")
	@Valid
	@ArraySchema(schema = @Schema(implementation = CreateProductRequestDto.class))
	List<CreateProductRequestDto> products
) {
}
