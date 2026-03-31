package jabaclass.settlement.domain.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import jabaclass.settlement.domain.model.Settlement;
import jabaclass.settlement.domain.model.SettlementStatus;

public interface SettlementRepository {

	Settlement save(Settlement settlement);

	List<Settlement> saveAll(List<Settlement> settlements);

	Optional<Settlement> findById(UUID settlementId);

	Optional<Settlement> findBySellerIdAndSettlementMonth(UUID sellerId, String settlementMonth);

	boolean existsBySellerIdAndSettlementMonth(UUID sellerId, String settlementMonth);

	List<Settlement> findByStatus(SettlementStatus status);

	List<Settlement> findBySettlementMonth(String settlementMonth);

	List<Settlement> findBySettlementMonthAndStatus(String settlementMonth, SettlementStatus status);
}