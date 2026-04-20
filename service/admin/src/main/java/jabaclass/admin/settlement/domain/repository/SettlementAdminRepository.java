package jabaclass.admin.settlement.domain.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import jabaclass.admin.settlement.domain.model.Settlement;

public interface SettlementAdminRepository {
	Page<Settlement> findAll(Pageable pageable);
}
