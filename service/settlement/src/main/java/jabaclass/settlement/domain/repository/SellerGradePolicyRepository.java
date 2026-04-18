package jabaclass.settlement.domain.repository;

import java.math.BigDecimal;
import java.util.Optional;

import jabaclass.settlement.domain.model.SellerGradePolicy;

public interface SellerGradePolicyRepository {

	SellerGradePolicy save(SellerGradePolicy sellerGradePolicy);

	Optional<SellerGradePolicy> findActiveApplicablePolicy(BigDecimal salesAmount);
}
