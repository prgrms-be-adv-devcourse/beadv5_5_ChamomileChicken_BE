package jabaclass.settlement.domain.repository;

import java.util.List;

import jabaclass.settlement.domain.model.grade.SellerGradePolicy;

public interface SellerGradePolicyRepository {

	List<SellerGradePolicy> findActivePolicies();
}
