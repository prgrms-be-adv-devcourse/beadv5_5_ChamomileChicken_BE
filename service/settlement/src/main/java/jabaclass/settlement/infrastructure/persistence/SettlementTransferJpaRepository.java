package jabaclass.settlement.infrastructure.persistence;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import jabaclass.settlement.domain.model.SettlementTransfer;

public interface SettlementTransferJpaRepository extends JpaRepository<SettlementTransfer, UUID> {
}
