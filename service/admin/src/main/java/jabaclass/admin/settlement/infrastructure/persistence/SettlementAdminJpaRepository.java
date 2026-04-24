package jabaclass.admin.settlement.infrastructure.persistence;

import java.math.BigDecimal;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jabaclass.admin.settlement.domain.model.Settlement;

public interface SettlementAdminJpaRepository extends JpaRepository<Settlement, UUID>, JpaSpecificationExecutor<Settlement> {

	@Query("SELECT COALESCE(SUM(s.settlementAmount), 0) FROM Settlement s WHERE s.settlementMonth = :month")
	BigDecimal sumSettlementAmountByMonth(@Param("month") String month);
}
