package jabaclass.admin.settlement.domain.repository;

import java.math.BigDecimal;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import jabaclass.admin.settlement.domain.dto.SettlementSearchCondition;
import jabaclass.admin.settlement.domain.model.Settlement;

public interface SettlementAdminRepository {
	Page<Settlement> findAll(SettlementSearchCondition condition, Pageable pageable);
	BigDecimal sumSettlementAmountByMonth(String month);
}
