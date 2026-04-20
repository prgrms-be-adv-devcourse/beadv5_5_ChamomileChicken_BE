package jabaclass.admin.settlement.infrastructure.persistence;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import jabaclass.admin.settlement.domain.model.Settlement;

public interface SettlementAdminJpaRepository extends JpaRepository<Settlement, UUID> {
}
