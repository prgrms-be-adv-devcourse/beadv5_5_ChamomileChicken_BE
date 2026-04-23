package jabaclass.admin.settlement.application.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jabaclass.admin.settlement.domain.dto.SettlementSearchCondition;
import jabaclass.admin.settlement.application.usecase.SettlementAdminUseCase;
import jabaclass.admin.settlement.domain.repository.SettlementAdminRepository;
import jabaclass.admin.settlement.presentation.dto.response.SettlementAdminResponseDto;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SettlementAdminService implements SettlementAdminUseCase {

	private final SettlementAdminRepository settlementAdminRepository;

	@Override
	@Transactional(readOnly = true)
	public Page<SettlementAdminResponseDto> getSettlements(Pageable pageable, SettlementSearchCondition condition) {
		return settlementAdminRepository.findAll(condition, pageable)
			.map(SettlementAdminResponseDto::from);
	}
}
