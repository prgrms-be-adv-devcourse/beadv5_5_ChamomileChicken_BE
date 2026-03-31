package jabaclass.user.user.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import jabaclass.user.user.domain.model.SellerSettlementAccount;
import jabaclass.user.user.domain.repository.SellerSettlementAccountRepository;
import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class SellerSettlementAccountRepositoryAdapter implements SellerSettlementAccountRepository {

	private final SellerSettlementAccountJpaRepository sellerSettlementAccountJpaRepository;

	@Override
	public Optional<SellerSettlementAccount> findByUserId(UUID userId) {
		return sellerSettlementAccountJpaRepository.findByUserId(userId);
	}

	@Override
	public List<SellerSettlementAccount> findAllByUserIds(List<UUID> userIds) {
		return sellerSettlementAccountJpaRepository.findAllByUserIdIn(userIds);
	}
}
