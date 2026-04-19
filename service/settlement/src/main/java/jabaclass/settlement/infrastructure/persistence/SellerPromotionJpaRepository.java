package jabaclass.settlement.infrastructure.persistence;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jabaclass.settlement.domain.model.SellerPromotion;

public interface SellerPromotionJpaRepository extends JpaRepository<SellerPromotion, UUID> {

	@Query("""
		select sp
		from SellerPromotion sp
		where sp.sellerId = :sellerId
		  and sp.active = true
		  and sp.startedAt <= :occurredAt
		  and (sp.endedAt is null or sp.endedAt >= :occurredAt)
		order by sp.startedAt desc
		""")
	List<SellerPromotion> findActiveApplicablePromotions(
		@Param("sellerId") UUID sellerId,
		@Param("occurredAt") LocalDateTime occurredAt
	);
}
