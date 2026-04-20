package jabaclass.admin.settlement.application.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jabaclass.admin.settlement.application.usecase.SettlementAdminUseCase;
import jabaclass.admin.settlement.domain.repository.SettlementAdminRepository;
import jabaclass.admin.settlement.presentation.dto.response.SettlementAdminResponseDto;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SettlementAdminService implements SettlementAdminUseCase {

	private final SettlementAdminRepository settlementAdminRepository;

	@Override
	@Transactional(readOnly = true, transactionManager = "settlementTransactionManager")
	public Page<SettlementAdminResponseDto> getSettlements(Pageable pageable) {
		return settlementAdminRepository.findAll(pageable)
			.map(SettlementAdminResponseDto::from);
	}
}
