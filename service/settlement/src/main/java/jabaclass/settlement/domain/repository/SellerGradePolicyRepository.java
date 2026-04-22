package jabaclass.settlement.domain.repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import jabaclass.settlement.domain.model.grade.SellerGradePolicy;

public interface SellerGradePolicyRepository {

	Optional<SellerGradePolicy> findActiveApplicablePolicy(BigDecimal salesAmount);

	List<SellerGradePolicy> findActivePolicies();
}
