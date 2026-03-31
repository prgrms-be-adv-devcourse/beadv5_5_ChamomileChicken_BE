package jabaclass.user.user.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import jabaclass.user.user.domain.model.SellerSettlementAccount;

public interface SellerSettlementAccountJpaRepository extends JpaRepository<SellerSettlementAccount, UUID> {

	Optional<SellerSettlementAccount> findByUserId(UUID userId);

	List<SellerSettlementAccount> findAllByUserIdIn(List<UUID> userIds);
}
