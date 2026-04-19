package jabaclass.settlement.domain.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import jabaclass.settlement.domain.model.settlement.Settlement;
import jabaclass.settlement.domain.model.settlement.SettlementStatus;

public interface SettlementRepository {

	List<Settlement> saveAll(List<Settlement> settlements);

	Optional<Settlement> findById(UUID settlementId);

	boolean existsBySellerIdAndSettlementMonth(UUID sellerId, String settlementMonth);

	List<Settlement> findBySettlementMonth(String settlementMonth);

	List<Settlement> findBySettlementMonthAndStatus(String settlementMonth, SettlementStatus status);
}
