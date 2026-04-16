package jabaclass.admin.settlement.infrastructure.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import jabaclass.admin.settlement.domain.model.Settlement;
import jabaclass.admin.settlement.domain.repository.SettlementAdminRepository;
import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class SettlementAdminRepositoryAdapter implements SettlementAdminRepository {

	private final SettlementAdminJpaRepository settlementAdminJpaRepository;

	@Override
	public Page<Settlement> findAll(Pageable pageable) {
		return settlementAdminJpaRepository.findAll(pageable);
	}
}
