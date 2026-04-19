package jabaclass.settlement.infrastructure.persistence;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import jabaclass.settlement.domain.model.SettlementPromotion;

public interface SettlementPromotionJpaRepository extends JpaRepository<SettlementPromotion, UUID> {
}
