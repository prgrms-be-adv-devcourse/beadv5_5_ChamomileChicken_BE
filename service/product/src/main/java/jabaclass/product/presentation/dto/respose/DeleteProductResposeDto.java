package jabaclass.product.presentation.dto.respose;

import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;
import jabaclass.product.domain.model.status.ProductStatus;
import jakarta.validation.constraints.NotNull;

@Schema(description = "상품 삭제 응답")
public record DeleteProductResposeDto(
	@NotNull(message = "상품 Id를 입력해주세요.")
	@Schema(description = "상품 ID", example = "11111111-1111-1111-1111-111111111111")
	UUID productId,

	@Schema(description = "상태", example = "ENABLE")
	ProductStatus status
) {

	public static DeleteProductResposeDto from(UUID productId, ProductStatus status) {
		return new DeleteProductResposeDto(
			productId, status
		);
	}

}
