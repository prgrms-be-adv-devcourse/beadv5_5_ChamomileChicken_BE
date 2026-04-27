package jabaclass.settlement.domain.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import jabaclass.settlement.domain.model.settlement.Settlement;
import jabaclass.settlement.domain.model.settlement.SettlementStatus;

public interface SettlementRepository {

	Settlement save(Settlement settlement);

	List<Settlement> saveAll(List<Settlement> settlements);

	Optional<Settlement> findById(UUID settlementId);

	List<Settlement> findBySettlementMonthAndSellerIds(String settlementMonth, List<UUID> sellerIds);

	Page<Settlement> findBySellerId(UUID sellerId, Pageable pageable);

	List<Settlement> findBySettlementMonth(String settlementMonth);

	List<Settlement> findBySettlementMonthAndStatus(String settlementMonth, SettlementStatus status);
}
