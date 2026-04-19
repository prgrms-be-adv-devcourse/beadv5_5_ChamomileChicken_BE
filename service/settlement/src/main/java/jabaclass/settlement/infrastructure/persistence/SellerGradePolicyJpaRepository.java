package jabaclass.settlement.infrastructure.persistence;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jabaclass.settlement.domain.model.grade.SellerGradePolicy;

public interface SellerGradePolicyJpaRepository extends JpaRepository<SellerGradePolicy, UUID> {

	@Query("""
		select p
		from SellerGradePolicy p
		where p.active = true
		  and p.minSalesAmount <= :salesAmount
		  and (p.maxSalesAmount is null or p.maxSalesAmount >= :salesAmount)
		order by p.minSalesAmount desc
		""")
	List<SellerGradePolicy> findActiveApplicablePolicies(@Param("salesAmount") BigDecimal salesAmount);
}
