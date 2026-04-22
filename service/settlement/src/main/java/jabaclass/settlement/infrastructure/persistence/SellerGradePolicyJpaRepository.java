package jabaclass.settlement.infrastructure.persistence;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import jabaclass.settlement.domain.model.grade.SellerGradePolicy;

public interface SellerGradePolicyJpaRepository extends JpaRepository<SellerGradePolicy, UUID> {

	List<SellerGradePolicy> findByActiveTrueOrderByMinSalesAmountDesc();
}
