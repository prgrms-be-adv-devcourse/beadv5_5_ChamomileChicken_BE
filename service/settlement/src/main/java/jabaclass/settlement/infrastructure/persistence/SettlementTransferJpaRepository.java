package jabaclass.settlement.infrastructure.persistence;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import jabaclass.settlement.domain.model.settlement.SettlementTransfer;

public interface SettlementTransferJpaRepository extends JpaRepository<SettlementTransfer, UUID> {

	Optional<SettlementTransfer> findTopBySettlementIdOrderByRequestedAtDesc(UUID settlementId);
}
