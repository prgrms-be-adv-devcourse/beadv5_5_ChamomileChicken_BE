package jabaclass.settlement.infrastructure.persistence;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import jabaclass.settlement.domain.model.settlement.SettlementTarget;

public interface SettlementTargetJpaRepository extends JpaRepository<SettlementTarget, UUID> {

	List<SettlementTarget> findByPaymentIdInAndTargetType(
		List<UUID> paymentIds,
		jabaclass.settlement.domain.model.settlement.SettlementTargetType targetType
	);

	List<SettlementTarget> findByIdIn(List<UUID> ids);
}
