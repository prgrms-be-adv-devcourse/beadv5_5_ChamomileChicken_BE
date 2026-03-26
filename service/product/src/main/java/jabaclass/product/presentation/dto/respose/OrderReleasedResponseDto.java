package jabaclass.product.presentation.dto.respose;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "재고 추가/감 응답")
public record OrderReleasedResponseDto(
	boolean released
) {
}
