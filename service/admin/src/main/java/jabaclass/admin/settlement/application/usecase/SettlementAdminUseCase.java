package jabaclass.admin.settlement.application.usecase;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import jabaclass.admin.settlement.presentation.dto.response.SettlementAdminResponseDto;

public interface SettlementAdminUseCase {

	Page<SettlementAdminResponseDto> getSettlements(Pageable pageable);

}
