package jabaclass.user.user.presentation.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UpsertSellerSettlementAccountRequestDto(
	@NotBlank(message = "은행 코드는 필수입니다.")
	String bankCode,

	@NotBlank(message = "계좌번호는 필수입니다.")
	String accountNumber,

	@NotBlank(message = "예금주는 필수입니다.")
	String accountHolder,

	@NotNull(message = "정산 계좌 활성 여부는 필수입니다.")
	Boolean active
) {
}
