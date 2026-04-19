package jabaclass.settlement.domain.repository;

import java.math.BigDecimal;
import java.util.Optional;

import jabaclass.settlement.domain.model.SellerGradePolicy;

public interface SellerGradePolicyRepository {

	Optional<SellerGradePolicy> findActiveApplicablePolicy(BigDecimal salesAmount);
}
