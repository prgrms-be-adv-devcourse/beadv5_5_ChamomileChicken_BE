package jabaclass.settlement.domain.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import jabaclass.settlement.domain.model.settlement.SettlementTarget;
import jabaclass.settlement.domain.model.settlement.SettlementTargetType;

public interface SettlementTargetRepository {

	List<SettlementTarget> saveAll(List<SettlementTarget> settlementTargets);

	SettlementTarget save(SettlementTarget settlementTarget);

	List<SettlementTarget> findByPaymentIdsAndTargetType(List<UUID> paymentIds, SettlementTargetType targetType);

	List<SettlementTarget> findAllByIds(List<UUID> ids);
}
