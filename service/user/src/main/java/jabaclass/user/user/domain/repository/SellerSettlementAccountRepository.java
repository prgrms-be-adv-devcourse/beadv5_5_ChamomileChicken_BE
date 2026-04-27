package jabaclass.user.user.domain.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import jabaclass.user.user.domain.model.SellerSettlementAccount;

public interface SellerSettlementAccountRepository {

	Optional<SellerSettlementAccount> findByUserId(UUID userId);

	List<SellerSettlementAccount> findAllByUserIds(List<UUID> userIds);

	SellerSettlementAccount save(SellerSettlementAccount sellerSettlementAccount);
}
